package com.may.ple.kyschkpay;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
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

public class KYSApi {
	private static final Logger LOG = Logger.getLogger(KYSApi.class.getName());
	public static final String LINK = "https://www.e-studentloan.ktb.co.th";
	private static String captchaPath = "D:/DMS_DATA/upload/temp/";
	private static final KYSApi instance = new KYSApi();
	
	private KYSApi(){}
	
	public static KYSApi getInstance(){
        return instance;
    }
	
	public static void main(String[] args) {
		try {
			System.out.println("##### Start " + String.format("%1$tH:%1$tM:%1$tS", Calendar.getInstance()));
			
			int x = 0;
			int poolSize = 1000;
			ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(poolSize);
			
			while(true) {
				System.out.println("============== " + executor.getQueue().size());
				while(executor.getQueue().size() == poolSize) {
					LOG.info("Pool size full : " + executor.getQueue().size());						
					Thread.sleep(1000);
				}
				
				executor.execute(new Test(x));
				x++;
				
				if(x == 1000) break;
			}
			
			System.out.println("#########");
			
			executor.shutdown();
			
			while(!executor.isTerminated()){}
			
			System.out.println("##### End " + String.format("%1$tH:%1$tM:%1$tS", Calendar.getInstance()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	public LoginRespModel login(String cid, String birthdate) throws Exception {
		String captchaFullPath = null;
		
		try {
			LOG.debug("Start login");
			
			//[1]
			Map<String, String> loginResp = getLoginPage();
			
			if(loginResp == null) {
				LoginRespModel resp = new LoginRespModel();
				resp.setStatus(StatusConstant.SERVICE_UNAVAILABLE);
				return resp;
			}
			
			//[2]
			captchaFullPath = loginResp.get("CAPTCHA_FULL_PATH");
			
//			JsonObject jsonObj = DMSApi.getInstance().img2txt(captchaFullPath);
//			String captchaTxt = jsonObj.get("text").getAsString();
//			LOG.debug("captchaTxt : "+ captchaTxt);
			
			String text = CaptchaResolve.tesseract(captchaFullPath);
			LOG.debug("captchaTxt : "+ text);
			
			
			//[3]
			String sessionId = loginResp.get("JSESSIONID");
			LoginRespModel resp = doLogin(sessionId, text, cid, birthdate);
			StatusConstant status = resp.getStatus();
			
			if(status == StatusConstant.LOGIN_SUCCESS) {
				resp.setSessionId(sessionId);
			} else if (status == StatusConstant.LOGIN_FAIL) {
				resp.setStatus(StatusConstant.LOGIN_FAIL);
			} else {
				resp.setStatus(StatusConstant.SERVICE_UNAVAILABLE);
			}
			
			return resp;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		} finally {
			if(StringUtils.isNotBlank(captchaFullPath)) {
				FileUtils.forceDelete(new File(captchaFullPath));
			}
		}
	}
	
	public List<String> getParam(String sessionId, String cif) throws Exception {
		try {
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLINQ008.do")
					.method(Method.POST)
					.data("cif", cif)
					.header("Content-Type", "application/x-www-form-urlencoded")
					.cookie("JSESSIONID", sessionId)
					.postDataCharset("UTF-8")
					.execute();
			
			Document doc = res.parse();
			Elements body = doc.select("body");
			String onload = body.get(0).attr("onload");
			
			if(StringUtils.isNoneBlank(onload) && onload.toLowerCase().contains("login")) {
				throw new CustomException(1, "Session Timeout");
			}
			
			//[]
			Elements tr = doc.select("table #td0");
			
			if(tr.size() == 0) throw new Exception("Not found [table #td0]");
			
			String onclick = tr.get(0).attr("onclick");
			LOG.debug("Get parameter from onclick : " + onclick);
			
			List<String> args = submitDataByGFDecode(onclick);
			LOG.debug(args);
			
			/*
			String loanType = args.get(0).trim();
			String loanName = args.get(1).trim();
			String accNo = args.get(2).trim();
			String accName = args.get(3).trim();
			String loanAccStatus = args.get(4).trim();
			String flag = args.get(5).trim();
			*/
			
			return args;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public void getPaymentInfo(String sessionId, String cif, String uri, String loanType, String accNo) throws Exception {
		try {
			Response res = Jsoup.connect(LINK + uri)
					.method(Method.POST)
					.data("loanType", loanType)
					.data("accNo", accNo)
					.data("cif", cif)
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
			Elements captchaEl;
			
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
	
	private LoginRespModel doLogin(String sessionId, String captcha, String cid, String birthdate) throws Exception {
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
			Elements cifEl = doc.select("td input[name='cif']");
//			Elements cidEl = doc.select("td input[name='cid']");
//			Elements cusName = doc.select("td input[name='stuFullName']");
			StatusConstant status;
			String cif = null;
			
			if(cifEl != null && StringUtils.isNoneBlank((cif = cifEl.val()))) {				
				status = StatusConstant.LOGIN_SUCCESS;
			} else if(doc.select("#capId") != null) {
				status = StatusConstant.LOGIN_FAIL;
			} else {
				status = StatusConstant.SERVICE_UNAVAILABLE;
			}
			
			LoginRespModel resp = new LoginRespModel();
			resp.setStatus(status);
			resp.setCif(cif);
			
			return resp;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private List<String> submitDataByGFDecode(String str) {
		try {
			int firstBracket = str.indexOf("(") + 1;
			int lastBracket = str.lastIndexOf(")");
			String rest = str.substring(firstBracket, lastBracket).replace("'", "");
			
			return Arrays.asList(rest.split(","));
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}







class Test implements Runnable {
	private int x;
	
	public Test(int x) {
		this.x = x;
	}

	@Override
	public void run() {
		try {
			System.out.println("Start " + x + " " + String.format("%1$tH:%1$tM:%1$tS", Calendar.getInstance()));
			
			byte[] bytes = FileUtils.readFileToByteArray(new File("D:\\python_captcha\\Captcha.jpg"));
			String imgBase64 = Base64.encodeBase64String(bytes);
			
			
			String text = CaptchaResolve.tesseract(imgBase64);
			System.out.println("captchaTxt : "+ text);
			
			System.out.println("End " + x + " " + String.format("%1$tH:%1$tM:%1$tS", Calendar.getInstance()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
