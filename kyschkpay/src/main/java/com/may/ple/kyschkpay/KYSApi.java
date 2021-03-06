package com.may.ple.kyschkpay;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class KYSApi {
	private static final Logger LOG = Logger.getLogger(KYSApi.class.getName());
	public static final String LINK = "https://www.e-studentloan.ktb.co.th";
	private static final KYSApi instance = new KYSApi();
	private static final int CONN_TIMEOUT = 30000;
	
	private KYSApi(){
		String proxyAuth = App.prop.getProperty("proxy_auth");
		if(StringUtils.isNotBlank(proxyAuth)) {
			final String[] proxyAuthArr = proxyAuth.split(":");
			
			Authenticator.setDefault(
			   new Authenticator() {
			      public PasswordAuthentication getPasswordAuthentication() {
			         return new PasswordAuthentication(
			        	proxyAuthArr[0], proxyAuthArr[1].toCharArray()
			         );
			      }
			   }
			);
		}
	}
	
	public static KYSApi getInstance(){
        return instance;
    }
	
	public Map<String, String> firstLogin(Proxy proxy, String email, String password) throws Exception {		
		try {
			LOG.debug("Start FirstLogin");
			
			//[1]	
			String sessionId = getESLLandPage(proxy);
			LOG.debug("sessionId: " + sessionId);
			if(sessionId == null) return null;
			
			//[2]
			LOG.debug("getFristLoginPage");
			String captchaUrl = getFristLoginPage(proxy, sessionId);
			if(captchaUrl == null) return null;
			
			//[3]
			Map<String, String> resp = null;
			String captcha;
			int x = 0;
			while(resp == null) {
				if(x == 5) return null;
				
				//[3.1]
				captcha = parseCaptcha(proxy, sessionId, captchaUrl);
				if(StringUtils.isBlank(captcha)) return null;
				
				LOG.info("Do firstLogin round: " + x);
				
				//[3.2]
				resp = doFirstLogin(proxy, sessionId, captcha, email, password);
				if(resp == null) {
					LOG.warn("Wait to re doFirstLogin.");
					Thread.sleep(500);
				}
				x++;
			}
			
			return resp;
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	public LoginRespModel secondLogin(Proxy proxy, String cid, String birthdate, Map<String, String> secondLoginPage) throws Exception {		
		try {
			
			LOG.debug("Start SecondLogin");
			String captchaUrl = secondLoginPage.get("captchaUrl");
			LoginRespModel resp = null;
			String captcha;
			int x = 0;
			while(resp == null || resp.getStatus() == StatusConstant.LOGIN_FAIL) {
				if(x == 3) return resp;
				
				//[1]
				captcha = parseCaptcha(proxy, secondLoginPage.get("sessionId"), captchaUrl);
				if(StringUtils.isBlank(captcha)) {
					resp = new LoginRespModel();
					resp.setStatus(StatusConstant.SERVICE_UNAVAILABLE);
					return resp;
				}
				
				LOG.info("Do secondLogin round: " + x);
				
				//[2]
				resp = doSecondLogin(proxy, secondLoginPage.get("sessionId"), captcha, cid, birthdate);
				LOG.info((proxy != null ? proxy.toString() : "No Proxy") + " " + resp.getStatus() + " round: " + x);
				
				if(resp.getStatus() == StatusConstant.SERVICE_UNAVAILABLE) {
					Thread.sleep(1000);
					String sessionId = secondLoginPage.get("sessionId");
					if(ManageLoginWorkerThread.firstLoginGate(
							proxy, 
							secondLoginPage.get("username"), 
							secondLoginPage.get("password"), 
							sessionId, 
							secondLoginPage
							) == null) {
						LOG.error("Re 1st login FAIL.");
					} else {
						LOG.info("Re 1st login SUCCESS.");
					}
				}
				
				x++;
			}
			
			LOG.info("End secondLogin : " + resp.getStatus());
			return resp;
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	public PaymentModel refresh(Proxy proxy, String sessionId, String cif) throws Exception {
		try {
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLINQ008.do")
					.proxy(proxy)
					.timeout(CONN_TIMEOUT)
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
			
			PaymentModel paymentModel = new PaymentModel();
			Elements title = doc.select("title");
			if(title.get(0).html().toUpperCase().equals("ERROR")) {
				paymentModel.setError(true);
			}
			return paymentModel;
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	public List<List<String>> getParam(Proxy proxy, String sessionId, String cif) throws Exception {
		try {
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLINQ008.do")
					.proxy(proxy)
					.timeout(CONN_TIMEOUT)
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
			
			List<List<String>> argsList = new ArrayList<>();
			String onclick;
			List<String> args;
			int index = 1;
			while(tr.size() > 0) {
				onclick = tr.get(0).attr("onclick");
				LOG.debug("Get parameter from onclick : " + onclick);
				
				args = submitDataByGFDecode(onclick);
				LOG.debug(args);
				
				/*
				String loanType = args.get(0).trim();
				String loanName = args.get(1).trim();
				String accNo = args.get(2).trim();
				String accName = args.get(3).trim();
				String loanAccStatus = args.get(4).trim();
				String flag = args.get(5).trim();
				*/
				
				argsList.add(args);
				tr = doc.select("table #td" + (index++));
			}
			
			return argsList;
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	public PaymentModel getPaymentInfo(Proxy proxy, String sessionId, String cif, String url, String loanType, String accNo) throws Exception {
		try {
			if(!App.checkWorkingHour()) {
				PaymentModel paymentModel = refresh(proxy, sessionId, cif);
				paymentModel.setRefresh(true);
				return paymentModel;
			}
			
			LOG.debug("Call getPaymentInfoPage");
			Document doc = getPaymentInfoPage(proxy, url, loanType, accNo, cif, sessionId);
			
			PaymentModel paymentModel = new PaymentModel();
			Elements tab1El = doc.select("#tab1");
			if(tab1El.size() == 0) {
				Elements title = doc.select("title");
				if(title.get(0).html().toUpperCase().equals("ERROR")) {
					paymentModel.setError(true);
					return paymentModel;
				}
				
				Elements body = doc.select("body");
				String onload = body.get(0).attr("onload");
				if(StringUtils.isNoneBlank(onload) && onload.toLowerCase().contains("login")) {
					throw new CustomException(1, "Session Timeout.");
				}
				
				if(StringUtils.isNoneBlank(onload) && onload.toLowerCase().contains("eslland.jsp")) {
					LOG.error("Session have problem.");
					paymentModel.setError(true);
					paymentModel.setReFirstLogin(true);
					paymentModel.setSessionId(sessionId);
					return paymentModel;
				}
				
				throw new Exception("Unknown Error accNo: " + accNo);
			}

			Elements preBalanceEl = tab1El.select("input[name='preBalance']");			
			Elements lastPaymentDateEl = tab1El.select("input[name='lastPaymentDate']");
			Elements lastPaymentAmountEl = tab1El.select("input[name='lastPaymentAmount']");
			
			Elements totalPaymentInstallmentStrEl = tab1El.select("input[name='totalPaymentInstallmentStr']");
			if(totalPaymentInstallmentStrEl.size() == 0) {
				totalPaymentInstallmentStrEl = tab1El.select("input[name='totalAdvancePay']");
			}
			
			paymentModel.setLastPayDate(strToDate(lastPaymentDateEl.get(0).val().trim()));
			paymentModel.setLastPayAmount(Double.valueOf(lastPaymentAmountEl.get(0).val().replace(",", "").trim()));
			paymentModel.setTotalPayInstallment(Double.valueOf(totalPaymentInstallmentStrEl.get(0).val().replace(",", "").trim()));
			paymentModel.setPreBalance(Double.valueOf(preBalanceEl.get(0).val().replace(",", "").trim()));
			paymentModel.setDoc(doc);
			
			return paymentModel;
		} catch (IOException e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			PaymentModel paymentModel = new PaymentModel();
			paymentModel.setError(true);
			return paymentModel;
		} catch (Exception e) {
			throw e;
		}
	}
	
	public Document getPaymentInfoPage(Proxy proxy, String url, String loanType, String accNo, String cif, String sessionId) throws Exception {
		try {
			LOG.debug("getPaymentInfoPage");
			Response res = Jsoup.connect(url)
					.proxy(proxy)
					.timeout(CONN_TIMEOUT)
					.method(Method.POST)
					.data("loanType", loanType)
					.data("accNo", accNo)
					.data("cif", cif)
					.data("browser", "Fire Fox Or Other")
					.header("Content-Type", "application/x-www-form-urlencoded")
					.cookie("JSESSIONID", sessionId)
					.postDataCharset("UTF-8")
					.execute();
			
			return res.parse();
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public byte[] getLoginPage(Proxy proxy, Response resp) throws Exception {
		try {
			Document doc = resp.parse();
			Elements captchaEl;
			
			if((captchaEl = doc.select("#capId")) == null || captchaEl.size() == 0) {
				return null;
			}
			
			String captchaImgUrl = LINK + captchaEl.get(0).attr("src");
			
			return getCaptchaImg(proxy, resp.cookies().get("JSESSIONID"), captchaImgUrl, null);
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}

	public byte[] getCaptchaImg(Proxy proxy, String sessionId, String captchaImgUrl, String cmd) throws Exception {
		try {
			LOG.debug("Start getCaptchaImg");
			Response res;
			byte image[];
			
			if(!StringUtils.isBlank(cmd)) {
				res = Jsoup
						.connect(captchaImgUrl) 	// Extract image absolute URL
						.proxy(proxy)
						.timeout(CONN_TIMEOUT)
						.data("cmd", cmd)
						.cookie("JSESSIONID", sessionId) 			// Grab cookies
						.ignoreContentType(true) 	// Needed for fetching image
						.method(Method.POST).execute();
				
				image = Base64.decodeBase64(res.bodyAsBytes());
			} else {
				// Fetch the captcha image
				res = Jsoup
						.connect(captchaImgUrl) 	// Extract image absolute URL
						.proxy(proxy)
						.timeout(CONN_TIMEOUT)
						.cookie("JSESSIONID", sessionId) 			// Grab cookies
						.ignoreContentType(true) 	// Needed for fetching image
						.execute();
				
				image = res.bodyAsBytes();
			}
	
//			UUID uuid = Generators.timeBasedGenerator().generate();
//			String captchaFullPath = captchaPath + uuid + ".jpg";
			
			// Load image from Jsoup response
			
			//ImageIO.write(ImageIO.read(new ByteArrayInputStream(image)), "jpg", new File("D:/captcha.jpg"));
			
			return image;
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	public String getESLLandPage(Proxy proxy) throws Exception {
		try {
			LOG.debug("Start getLoginPage");
			
			Response res = Jsoup
					.connect(LINK + "/STUDENT/jsp/ESLLand.jsp")
					.proxy(proxy)
					.timeout(CONN_TIMEOUT)
					.method(Method.GET).execute();
			Map<String, String> cookie = res.cookies();
			Document doc = res.parse();
			Elements eslloginEl;
			
			if((eslloginEl = doc.select("input[id='esllogin']")) == null || eslloginEl.size() == 0) {
				return null;
			}
			
			return cookie.get("JSESSIONID");
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	private String getFristLoginPage(Proxy proxy, String sessionId) throws Exception {
		try {
			LOG.debug("Start getLoginPage");
			
			Response res = Jsoup
					.connect(LINK + "/STUDENT/ESLLand.do")
					.proxy(proxy)
					.data("actionType", "1")
					.cookie("JSESSIONID", sessionId)
					.timeout(CONN_TIMEOUT)
					.method(Method.POST).execute();
			
			Document doc = res.parse();
			Elements captchaEl;
			
			if((captchaEl = doc.select("#capId")) == null || captchaEl.size() == 0) {
				LOG.warn("Not found #capId on html");
				return null;
			}
			
			return LINK + captchaEl.get(0).attr("src");
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	private Map<String, String> doFirstLogin(Proxy proxy, String sessionId, String captcha, String email, String password) throws Exception {
		try {
			LOG.debug("Start doLogin");
			
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLLogin.do")
					.timeout(CONN_TIMEOUT)
					.proxy(proxy)
					.method(Method.POST)
					.data("email", email)
					.data("password", password)
					.data("captcharL", captcha)
					.data("flag", "S")
					.header("Content-Type", "application/x-www-form-urlencoded")
					.cookie("JSESSIONID", sessionId)
					.postDataCharset("UTF-8")
					.execute();
			
			Map<String, String> cookies = res.cookies();
			cookies.put("JSESSIONID", sessionId);
			
			Document doc = res.parse();
			Elements captchaEl, cidEl, emailEl;
			
			if((cidEl = doc.select("input[name='cid']")) != null && cidEl.size() > 0) {
				if((captchaEl = doc.select("#capId")) == null || captchaEl.size() == 0) {
					LOG.error("Do firstLogin NOT found #capId on html.");
					return null;
				}
			} else if((emailEl = doc.select("input[name='email']")) != null && emailEl.size() > 0) {
				// re first login.
				LOG.warn("Do firstLogin fail.");
				Thread.sleep(1000);
				return null;
			} else {
				// others error.
				LOG.warn("Do firstLogin NOT found even email tag.");
				Thread.sleep(1000);
				return null;
			}
			
			String captchaUrl = LINK + captchaEl.get(0).attr("src");
			
			Map<String, String> resp = new HashMap<>();
			resp.put("captchaUrl", captchaUrl);
			resp.put("sessionId", sessionId);
			
			return resp;
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	private LoginRespModel doSecondLogin(Proxy proxy, String sessionId, String captcha, String cid, String birthdate) throws Exception {
		try {
			LOG.debug("Start doLogin");
			
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLLoginSp.do")
					.timeout(CONN_TIMEOUT)
					.proxy(proxy)
					.method(Method.POST)
					.data("cid", cid)
					.data("stuBirthdate", birthdate)
					.data("captcha", captcha)
					.data("flag", "S")
					.header("Content-Type", "application/x-www-form-urlencoded")
					.cookie("JSESSIONID", sessionId)
					.postDataCharset("UTF-8")
					.execute();
			
			Document doc = res.parse();
			Elements cifEl = doc.select("td input[name='cif']"), capIdEl;
//			Elements cidEl = doc.select("td input[name='cid']");
//			Elements cusName = doc.select("td input[name='stuFullName']");
			StatusConstant status;
			String cif = null;
			
			if(cifEl != null && StringUtils.isNoneBlank((cif = cifEl.val()))) {				
				status = StatusConstant.LOGIN_SUCCESS;
			} else if((capIdEl = doc.select("#capId")) != null && capIdEl.size() > 0) {
				status = StatusConstant.LOGIN_FAIL;
			} else {
				status = StatusConstant.SERVICE_UNAVAILABLE;
			}
			
			LoginRespModel resp = new LoginRespModel();
			resp.setStatus(status);
			resp.setCif(cif);
			
			return resp;
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	private List<String> submitDataByGFDecode(String str) {
		try {
			int firstBracket = str.indexOf("(") + 1;
			int lastBracket = str.lastIndexOf(")");
			String rest = str.substring(firstBracket, lastBracket).replace("'", "");
			String[] paramsStr = rest.split(",");
			List<String> params = new ArrayList<>();
			
			for (String param : paramsStr) {
				params.add(param.trim());
			}
			
			return params;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private Date strToDate(String dateStr) throws Exception {
		try {
			if(StringUtils.isBlank(dateStr)) return null;
			
			String[] split = dateStr.split("/");
			
			int dd = Integer.parseInt(split[0]);
			int mm = Integer.parseInt(split[1]) - 1;
			int yyyy = Integer.parseInt(split[2]) - 543;
			
			Calendar in = Calendar.getInstance();
			in.set(yyyy, mm, dd);
			
			return in.getTime();
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private String parseCaptcha(Proxy proxy, String sessionId, String captchaUrl) throws Exception {
		try {
			String captcha = "";
			byte[] captchaImg;
			int x = 0;
			
			while(StringUtils.isBlank(captcha)) {
				if(x == 10) break;
				
				if(x != 0) {
					Thread.sleep(500);
					captchaImg = getCaptchaImg(proxy, sessionId, captchaUrl, "rf");					
				} else {
					captchaImg = getCaptchaImg(proxy, sessionId, captchaUrl, null);
				}
				captcha = new Tess4jCaptcha(Tess4jCaptcha.DENOISE.CV).solve(captchaImg);
				LOG.info("Captcha: [" + captcha + "] of round: " + x);
				x++;
			}
			return captcha;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}