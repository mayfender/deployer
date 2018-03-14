package com.may.ple.kyschkpay;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
	
	public LoginRespModel login(Proxy proxy, String cid, String birthdate, int round) throws Exception {		
		try {
			LOG.debug("Start login");
			
			//[1]
			LoginRespModel loginResp = getLoginPage(proxy);
			
			if(loginResp == null) {
				loginResp = new LoginRespModel();
				loginResp.setStatus(StatusConstant.SERVICE_UNAVAILABLE);
				return loginResp;
			}
			
			//[2]
			String text = new Tess4jCaptcha(Tess4jCaptcha.DENOISE.CV).solve(loginResp.getImageContent());
//			String text = CaptchaResolve.tesseract(Base64.encodeBase64String(loginResp.getImageContent()));
//			String text = CaptchaResolve.captchatronix(loginResp.getImageContent());
			
			//[3]
			doLogin(proxy, loginResp, text, cid, birthdate);
			LOG.info((proxy != null ? proxy.toString() : "No Proxy") + " " + loginResp.getStatus() + " for " + text + " round: " + round);
			
			return loginResp;
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
					throw new CustomException(1, "Session Timeout");
				}
				
				throw new Exception("Unknown Error");
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

	public LoginRespModel getLoginPage(Proxy proxy) throws Exception {
		try {
			LOG.debug("Start getLoginPage");
			
			Response res = Jsoup
					.connect(LINK + "/STUDENT/ESLLogin.do")
					.proxy(proxy)
					.timeout(CONN_TIMEOUT)
					.method(Method.GET).execute();
			Map<String, String> cookie = res.cookies();
			Document doc = res.parse();
			Elements captchaEl;
			
			if((captchaEl = doc.select("#capId")) == null || captchaEl.size() == 0) {
				return null;
			}
			
			String captchaImgUrl = LINK + captchaEl.get(0).attr("src");
						
			LoginRespModel resp = new LoginRespModel();
			resp.setSessionId(cookie.get("JSESSIONID"));
			resp.setImageContent(getCaptchaImg(proxy, cookie, captchaImgUrl));
						
			return resp;
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}

	private byte[] getCaptchaImg(Proxy proxy, Map<String, String> cookie, String captchaImgUrl) throws Exception {
		try {
			LOG.debug("Start getCaptchaImg");
			
			// Fetch the captcha image
			Response res = Jsoup
					.connect(captchaImgUrl) 	// Extract image absolute URL
					.proxy(proxy)
					.timeout(CONN_TIMEOUT)
					.cookies(cookie) 			// Grab cookies
					.ignoreContentType(true) 	// Needed for fetching image
					.execute();
	
//			UUID uuid = Generators.timeBasedGenerator().generate();
//			String captchaFullPath = captchaPath + uuid + ".jpg";
			
			// Load image from Jsoup response
//			ImageIO.write(ImageIO.read(new ByteArrayInputStream(res.bodyAsBytes())), "jpg", new File(captchaFullPath));
			
			return res.bodyAsBytes();
		} catch (Exception e) {
			LOG.error((proxy != null ? proxy.toString() : "No Proxy") + " " + e.toString());
			throw e;
		}
	}
	
	private void doLogin(Proxy proxy, LoginRespModel loginResp, String captcha, String cid, String birthdate) throws Exception {
		try {
			LOG.debug("Start doLogin");
			
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLLogin.do")
					.timeout(CONN_TIMEOUT)
					.proxy(proxy)
					.method(Method.POST)
					.data("cid", cid)
					.data("stuBirthdate", birthdate)
					.data("captchar", captcha)
					.data("flag", "S")
					.header("Content-Type", "application/x-www-form-urlencoded")
					.cookie("JSESSIONID", loginResp.getSessionId())
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
			
			loginResp.setStatus(status);
			loginResp.setCif(cif);
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
			
			return Arrays.asList(rest.split(","));
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
	
}