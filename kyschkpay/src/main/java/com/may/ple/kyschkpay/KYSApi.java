package com.may.ple.kyschkpay;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.fasterxml.uuid.Generators;
import com.google.gson.JsonObject;

public class KYSApi {
	private enum LOGIN_STATUS {SERVICE_UNAVAILABLE, FAIL, SUCCESS};
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
			//[1]
			Map<String, String> loginResp = getLoginPage();
			
			if(loginResp == null) return "-1";
			
			//[2]
			String sessionId = loginResp.get("JSESSIONID");
			captchaFullPath = loginResp.get("CAPTCHA_FULL_PATH");
			JsonObject jsonObj = DMSApi.getInstance().img2txt(captchaFullPath);
			String captchaTxt = jsonObj.get("text").getAsString();
			System.out.println("captchaTxt : "+ captchaTxt);
			
			//[3]
			LOGIN_STATUS status = doLogin(sessionId, captchaTxt, cid, birthdate);
			if(status == LOGIN_STATUS.SUCCESS) {
				return sessionId;				
			} else if (status == LOGIN_STATUS.FAIL) {
				return "0";
			} else {
				return "-1";
			}
		} catch (Exception e) {
			FileUtils.deleteQuietly(new File(captchaFullPath));
			throw e;
		} finally {
			if(StringUtils.isNotBlank(captchaFullPath)) {
				FileUtils.forceDelete(new File(captchaFullPath));
			}
		}
	}

	private Map<String, String> getLoginPage() throws Exception {
		try {
			Response res = Jsoup
					.connect(LINK + "/STUDENT/ESLLogin.do")
					.method(Method.GET).execute();
			Map<String, String> cookie = res.cookies();
			Document doc = res.parse();
			Elements captchaEl = doc.select("#capId");
			
			if((captchaEl = doc.select("#capId")) == null) {
				return null;
			}
			
			String captchaImgUrl = LINK + captchaEl.get(0).attr("src");
			
			cookie.put("CAPTCHA_FULL_PATH", getCaptchaImg(cookie, captchaImgUrl));
						
			return cookie;
		} catch (Exception e) {
			throw e;
		}
	}

	private String getCaptchaImg(Map<String, String> cookie, String captchaImgUrl) throws Exception {
		try {
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
			throw e;
		}
	}
	
	private LOGIN_STATUS doLogin(String sessionId, String captcha, String cid, String birthdate) throws Exception {
		try {
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
			LOGIN_STATUS status;
			
			if(cusName != null && StringUtils.isNoneBlank(cusName.val())) {				
				status = LOGIN_STATUS.SUCCESS;
			} else if(doc.select("#capId") != null) {
				status = LOGIN_STATUS.FAIL;
			} else {
				status = LOGIN_STATUS.SERVICE_UNAVAILABLE;
			}			
			
			return status;
		} catch (Exception e) {
			throw e;
		}
	}
	
}
