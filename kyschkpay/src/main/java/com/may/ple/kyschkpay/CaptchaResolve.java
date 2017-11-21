package com.may.ple.kyschkpay;

import java.io.File;
import java.util.Calendar;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CaptchaResolve {
	private static final Logger LOG = Logger.getLogger(CaptchaResolve.class.getName());
	private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(10 * 1000).build();
	
	public static String tesseract(String imgBase64) throws Exception {
		try {			
			String baseDir = "D:/python_captcha";
			String tesseractPath = "C:/Program Files (x86)/Tesseract-OCR";
			String pythonPath = "C:\\Users\\sarawuti\\AppData\\Local\\Programs\\Python\\Python36-32";
			
			return CaptchaUtil.tesseract(imgBase64, baseDir, tesseractPath, pythonPath);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	public static String captchasolutions(String imgBase64) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start initData");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost("http://api.captchasolutions.com/solve");
			
			// build multipart upload request
            HttpEntity data = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addTextBody("p", "base64", ContentType.DEFAULT_BINARY)
                    .addTextBody("captcha", "data:image/jpg;base64," + imgBase64, ContentType.DEFAULT_BINARY)
                    .addTextBody("key", "d00ae078d7ace07e9cef2ba3e0b287db", ContentType.DEFAULT_BINARY)
                    .addTextBody("secret", "04efbb15", ContentType.DEFAULT_BINARY)
                    .build();
            
			httpPost.setEntity(data);			
			HttpResponse response = httpClient.execute(httpPost);
	        
			return xmlParser(response);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public static String captchatronix(String imgBase64) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start initData");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost("http://api.captchatronix.com/");
			
			/*List<NameValuePair> postParameters = new ArrayList<>();
		    postParameters.add(new BasicNameValuePair("username", "mayfender"));
		    postParameters.add(new BasicNameValuePair("password", "O4TIorHKqB"));
		    postParameters.add(new BasicNameValuePair("function", "picture2"));
		    postParameters.add(new BasicNameValuePair("pic", "https://www.e-studentloan.ktb.co.th/STUDENT/Captcha.jpg"));*/
			
			
			MultipartEntityBuilder multipart = MultipartEntityBuilder.create();
			multipart.addTextBody("username", "mayfender", ContentType.TEXT_PLAIN);
			multipart.addTextBody("password", "O4TIorHKqB", ContentType.TEXT_PLAIN);
			multipart.addTextBody("function", "picture2", ContentType.TEXT_PLAIN);
			File file = new File("D:\\python_captcha\\Captcha.jpg");
			
			/*HttpEntity data = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addTextBody("username", "mayfender", ContentType.DEFAULT_BINARY)
                    .addTextBody("password", "O4TIorHKqB", ContentType.DEFAULT_BINARY)
                    .addTextBody("function", "picture2", ContentType.DEFAULT_BINARY)
                    .addTextBody("pic", "data:image/jpg;base64," + imgBase64, ContentType.DEFAULT_BINARY)
                    .build();*/
		    
			httpPost.setEntity(data);			
			HttpResponse response = httpClient.execute(httpPost);
	        
			return entityStr(response);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public static void main(String[] args) {
		try {
			System.out.println(String.format("%1$tH:%1$tM:%1$tS", Calendar.getInstance().getTime()));
			
			/*File file = new File("D:\\python_captcha\\Captcha.jpg");
			String base64String = Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
			String string = tesseract(base64String);
			System.out.println(string);*/
			
			/*File file = new File("D:\\python_captcha\\download.jpg");
			String base64String = Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
			String result = CaptchaResolve.captchasolutions(base64String);
			System.out.println(result);*/
			
			File file = new File("D:\\python_captcha\\Captcha.jpg");
			String base64String = Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
			String txt = captchatronix(base64String);
			System.out.println(txt);
			
			System.out.println(String.format("%1$tH:%1$tM:%1$tS", Calendar.getInstance().getTime()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static JsonObject jsonParser(HttpResponse response) throws Exception {
		try {
			LOG.debug("Start jsonParser");
			String jsonStr = entityStr(response);
			JsonElement jsonElement =  new JsonParser().parse(jsonStr);
			return jsonElement.getAsJsonObject();
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private static String xmlParser(HttpResponse response) throws Exception {
		try {
			LOG.debug("Start jsonParser");
			String jsonStr = entityStr(response);
			Document doc = Jsoup.parse(jsonStr, "", Parser.xmlParser());
			Elements el = doc.select("decaptcha");
			
			return el.text();
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private static String entityStr(HttpResponse response) throws Exception {
		try {
			LOG.debug("Start jsonStr");
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
				   + response.getStatusLine().getStatusCode());
			}
			
			HttpEntity entity = response.getEntity();
			
			return entity != null ? EntityUtils.toString(entity) : null;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
