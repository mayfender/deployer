package com.may.ple.kyschkpay;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.uuid.Generators;
import com.google.gson.JsonObject;

public class KYSApi {
	private static final Logger LOG = Logger.getLogger(KYSApi.class.getName());
	private static String LINK = "https://www.e-studentloan.ktb.co.th";
	private static String captchaPath = "D:/DMS_DATA/upload/temp/";
	private static final KYSApi instance = new KYSApi();
	
	private KYSApi(){}
	
	public static KYSApi getInstance(){
        return instance;
    }
	
	public String login(String cid, String birthdate) throws Exception {
		String captchaFullPath = null;
		
		try {
			LOG.debug("Start login");
			
			//[1]
			Map<String, String> loginResp = getLoginPage();
			
			if(loginResp == null) return StatusConstant.SERVICE_UNAVAILABLE.getStatus().toString();
			
			//[2]
			String sessionId = loginResp.get("JSESSIONID");
			captchaFullPath = loginResp.get("CAPTCHA_FULL_PATH");
			JsonObject jsonObj = DMSApi.getInstance().img2txt(captchaFullPath);
			String captchaTxt = jsonObj.get("text").getAsString();
			LOG.debug("captchaTxt : "+ captchaTxt);
			
			//[3]
			StatusConstant status = doLogin(sessionId, captchaTxt, cid, birthdate);
			if(status == StatusConstant.LOGIN_SUCCESS) {
				return sessionId;				
			} else if (status == StatusConstant.LOGIN_FAIL) {
				return StatusConstant.LOGIN_FAIL.getStatus().toString();
			} else {
				return StatusConstant.SERVICE_UNAVAILABLE.getStatus().toString();
			}
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		} finally {
			if(StringUtils.isNotBlank(captchaFullPath)) {
				FileUtils.forceDelete(new File(captchaFullPath));
			}
		}
	}
	
	public void getPaymentInfo(String sessionId) throws Exception {
		try {
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLMTI001.do")
					.method(Method.POST)
					.data("loanType", "F101")
					.data("accNo", "1006277854")
					.data("cif", "")
					.data("browser", "Fire Fox Or Other")
					.header("Content-Type", "application/x-www-form-urlencoded")
					.cookie("JSESSIONID", sessionId)
					.postDataCharset("UTF-8")
					.execute();
			
			Document doc = res.parse();
			Elements table = doc.select("#tab4 table table");
			Elements rows = table.select("tr");
			Elements cols;
			boolean isFirstRow = true;
			
			for (Element row : rows) {
				cols = row.select("td");
				
				for (Element col : cols) {
					if(isFirstRow) {
						System.out.print(String.format("%25s", col.select("div").text() + "| "));
						isFirstRow = false;
					} else {
						System.out.print(String.format("%25s", col.text() + "| "));												
					}
				}
				System.out.println();
			}
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}

	private Map<String, String> getLoginPage() throws Exception {
		try {
			LOG.debug("Start getLoginPage");
			
			Response res = Jsoup
					.connect(LINK + "/STUDENT/ESLLogin.do")
					.method(Method.GET).execute();
			Map<String, String> cookie = res.cookies();
			Document doc = res.parse();
			Elements captchaEl = doc.select("#capId");
			
			if((captchaEl = doc.select("#capId")) == null || captchaEl.size() == 0) {
				return null;
			}
			
			String captchaImgUrl = LINK + captchaEl.get(0).attr("src");
			
			cookie.put("CAPTCHA_FULL_PATH", getCaptchaImg(cookie, captchaImgUrl));
						
			return cookie;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}

	private String getCaptchaImg(Map<String, String> cookie, String captchaImgUrl) throws Exception {
		try {
			LOG.debug("Start getCaptchaImg");
			
			// Fetch the captcha image
			Response res = Jsoup
					.connect(captchaImgUrl) 	// Extract image absolute URL
					.cookies(cookie) 			// Grab cookies
					.ignoreContentType(true) 	// Needed for fetching image
					.execute();
	
			UUID uuid = Generators.timeBasedGenerator().generate();
			String captchaFullPath = captchaPath + uuid + ".jpg";
			
			// Load image from Jsoup response
			ImageIO.write(ImageIO.read(new ByteArrayInputStream(res.bodyAsBytes())), "jpg", new File(captchaFullPath));
			
			return captchaFullPath;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private StatusConstant doLogin(String sessionId, String captcha, String cid, String birthdate) throws Exception {
		try {
			LOG.debug("Start doLogin");
			
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLLogin.do")
					.method(Method.POST)
					.data("cid", cid)
					.data("stuBirthdate", birthdate)
					.data("captchar", captcha)
					.data("flag", "S")
					.header("Content-Type", "application/x-www-form-urlencoded")
					.cookie("JSESSIONID", sessionId)
					.postDataCharset("UTF-8")
					.execute();
			
			Document doc = res.parse();
			Elements cusName = doc.select("td input[name='stuFullName']");
			StatusConstant status;
			
			if(cusName != null && StringUtils.isNoneBlank(cusName.val())) {				
				status = StatusConstant.LOGIN_SUCCESS;
			} else if(doc.select("#capId") != null) {
				status = StatusConstant.LOGIN_FAIL;
			} else {
				status = StatusConstant.SERVICE_UNAVAILABLE;
			}			
			
			return status;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
