package com.may.ple.kyschkpay;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
	
	private KYSApi(){}
	
	public static KYSApi getInstance(){
        return instance;
    }
	
	public LoginRespModel login(String cid, String birthdate) throws Exception {		
		try {
			LOG.debug("Start login");
			
			//[1]
			LoginRespModel loginResp = getLoginPage();
			
			if(loginResp == null) {
				loginResp = new LoginRespModel();
				loginResp.setStatus(StatusConstant.SERVICE_UNAVAILABLE);
				return loginResp;
			}
			
			//[2]
			String text = CaptchaResolve.tesseract(Base64.encodeBase64String(loginResp.getImageContent()));
//			String text = CaptchaResolve.captchatronix(loginResp.getImageContent());
			LOG.debug("captchaTxt : "+ text);
			
			//[3]
			doLogin(loginResp, text, cid, birthdate);
			
			return loginResp;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
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
	
	public PaymentModel getPaymentInfo(String sessionId, String cif, String url, String loanType, String accNo) throws Exception {
		try {
			Response res = Jsoup.connect(url)
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
			Elements lastPaymentDateEl = doc.select("input[name='lastPaymentDate']");
			
			if(lastPaymentDateEl.size() == 0) {
				throw new CustomException(1, "Session Timeout");
			}
			
			Elements lastPaymentAmountEl = doc.select("input[name='lastPaymentAmount']");
			Elements totalPaymentInstallmentStrEl = doc.select("input[name='totalPaymentInstallmentStr']");
			Elements preBalanceEl = doc.select("input[name='preBalance']");
			
			PaymentModel paymentModel = new PaymentModel();
			paymentModel.setLastPayDate(strToDate(lastPaymentDateEl.get(0).val()));
			paymentModel.setLastPayAmount(Double.valueOf(lastPaymentAmountEl.get(0).val().replace(",", "").trim()));
			paymentModel.setTotalPayInstallment(Double.valueOf(totalPaymentInstallmentStrEl.get(0).val().replace(",", "").trim()));
			paymentModel.setPreBalance(Double.valueOf(preBalanceEl.get(0).val().replace(",", "").trim()));
			
			return paymentModel;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}

	private LoginRespModel getLoginPage() throws Exception {
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
						
			LoginRespModel resp = new LoginRespModel();
			resp.setSessionId(cookie.get("JSESSIONID"));
			resp.setImageContent(getCaptchaImg(cookie, captchaImgUrl));
						
			return resp;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}

	private byte[] getCaptchaImg(Map<String, String> cookie, String captchaImgUrl) throws Exception {
		try {
			LOG.debug("Start getCaptchaImg");
			
			// Fetch the captcha image
			Response res = Jsoup
					.connect(captchaImgUrl) 	// Extract image absolute URL
					.cookies(cookie) 			// Grab cookies
					.ignoreContentType(true) 	// Needed for fetching image
					.execute();
	
//			UUID uuid = Generators.timeBasedGenerator().generate();
//			String captchaFullPath = captchaPath + uuid + ".jpg";
			
			// Load image from Jsoup response
//			ImageIO.write(ImageIO.read(new ByteArrayInputStream(res.bodyAsBytes())), "jpg", new File(captchaFullPath));
			
			return res.bodyAsBytes();
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private void doLogin(LoginRespModel loginResp, String captcha, String cid, String birthdate) throws Exception {
		try {
			LOG.debug("Start doLogin");
			
			Response res = Jsoup.connect(LINK + "/STUDENT/ESLLogin.do")
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
	
	private Date strToDate(String dateStr) throws Exception {
		try {
			String[] split = dateStr.split("/");
			int yyyy = Integer.parseInt(split[2]) - 543;
			
			return new SimpleDateFormat("ddMMyyyy").parse(new String(split[0] + split[1] + yyyy));
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}