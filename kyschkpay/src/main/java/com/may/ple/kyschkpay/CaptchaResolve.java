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

public class CaptchaResolve {
	private static final Logger LOG = Logger.getLogger(CaptchaResolve.class.getName());
	private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(10 * 1000).build();
	
	public static String tesseract(String imgBase64) throws Exception {
		try {
			String baseDir = App.prop.getProperty("python_script_dir");
			String tesseractPath = App.prop.getProperty("tesseract_path");
			String pythonPath = App.prop.getProperty("python_path");
			
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
	
	public static String captchatronix(byte[] captcha) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start initData");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost("http://api.captchatronix.com/");
			
			HttpEntity multipart = MultipartEntityBuilder.create()
			.addTextBody("username", "mayfender", ContentType.TEXT_PLAIN)
			.addTextBody("password", "O4TIorHKqB", ContentType.TEXT_PLAIN)
			.addTextBody("function", "picture2", ContentType.TEXT_PLAIN)
			.addBinaryBody("pict", captcha, ContentType.MULTIPART_FORM_DATA, "dummy.jpg").build();
		    
			httpPost.setEntity(multipart);			
			HttpResponse response = httpClient.execute(httpPost);
	        
			String result = entityStr(response);
			LOG.debug("captchaTxt : "+ result);
			String[] split = result.split("\\|");
			result = split[split.length - 1].trim();
			LOG.debug("captchaTxt final : "+ result);
			
			return result;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public static String captchaSniper(byte[] captcha) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start initData");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost("http://127.0.0.1");
			
			HttpEntity multipart = MultipartEntityBuilder.create()
			.addBinaryBody("pict", captcha, ContentType.MULTIPART_FORM_DATA, "dummy.jpg").build();
		    
			httpPost.setEntity(multipart);			
			HttpResponse response = httpClient.execute(httpPost);
	        
			String result = entityStr(response);
			LOG.debug("captchaTxt : "+ result);
			String[] split = result.split("\\|");
			result = split[split.length - 1].trim();
			LOG.debug("captchaTxt final : "+ result);
			
			return result;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public static void main(String[] args) {
		try {
			System.out.println(String.format("%1$tH:%1$tM:%1$tS", Calendar.getInstance().getTime()));
			
			final File file = new File("D:\\python_captcha\\Captcha2.jpg");
//			final File file = new File("C:\\Program Files (x86)\\CaptchaSniper517\\captchas\\typeu18\\captcha.png");
			
			
			
//			String txt = captchatronix(FileUtils.readFileToByteArray(file));
			
			/*byte[] data = FileUtils.readFileToByteArray(file);
			String base64String = Base64.encodeBase64String(data);
			
			String txt = tesseract(base64String);
			System.out.println(txt);*/
			int i = 0;
			while(true) {
				if(i == 1) break;
				
				new Thread() {
				    public void run() {
				    	try {
				    		String txt = captchaSniper(FileUtils.readFileToByteArray(file));
				    		System.out.println(this.getId() + " " + txt);							
						} catch (Exception e) {
							e.printStackTrace();
						}
				    }
				}.start();
				
				i++;
			}
			
			
			
			
			System.out.println(String.format("%1$tH:%1$tM:%1$tS", Calendar.getInstance().getTime()));
		} catch (Exception e) {
			e.printStackTrace();
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
