package com.may.ple.kyschkpay;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DMSApi {
	private static final Logger LOG = Logger.getLogger(DMSApi.class.getName());
	private static final DMSApi instance = new DMSApi();
	private final String BASE_URL = "http://127.0.0.1:8080/backend";
	private final RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
	private String token;
	
	private DMSApi() {}
	
	public static DMSApi getInstance(){
        return instance;
    }
	
	public boolean login(String username, String pass) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start Login");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost(BASE_URL + "/login");
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("username", username);
			jsonObject.addProperty("password", Base64.encodeBase64String(pass.getBytes()));
			
			StringEntity userEntity = new StringEntity(jsonObject.toString(), "utf8");
			httpPost.setEntity(userEntity);
			
			HttpResponse response = httpClient.execute(httpPost);
			
			JsonObject jsonObj = jsonParser(response);
			this.token = jsonObj.get("token").getAsString();
			
			LOG.debug("token : " + token);
			return true;
		} catch (Exception e) {
			LOG.error(e.toString());
			return false;
		} finally {
			try {
				if(httpClient != null) httpClient.close(); 
			} catch (Exception e2) {}
		}
	}
	
	public JsonObject initData(String productId) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start initData");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpGet httpPost = new HttpGet(BASE_URL + "/restAct/paymentOnlineCheck/initData?productId=" + productId);
			
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			httpPost.addHeader("X-Auth-Token", this.token);
			
			HttpResponse response = httpClient.execute(httpPost);
			return jsonParser(response);
		} catch (Exception e) {
			LOG.error(e.toString());
			return null;
		}
	}
	
	public JsonObject getChkList(String productId, int currentPage, int itemsPerPage, String workType) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start getChkList");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost(BASE_URL + "/restAct/paymentOnlineCheck/getCheckList");
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			httpPost.addHeader("X-Auth-Token", this.token);
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("productId", productId);
			jsonObject.addProperty("currentPage", currentPage);
			jsonObject.addProperty("itemsPerPage", itemsPerPage);
			jsonObject.addProperty("workType", workType);
			
			StringEntity userEntity = new StringEntity(jsonObject.toString(), "utf8");
			httpPost.setEntity(userEntity);
			
			HttpResponse response = httpClient.execute(httpPost);
			return jsonParser(response);
		} catch (Exception e) {
			LOG.error(e.toString());
			return null;
		}
	}
	
	public JsonObject img2txt(String captChaPath) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start img2txt");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost(BASE_URL + "/restAct/tools/img2txt");
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			httpPost.addHeader("X-Auth-Token", this.token);
			
			String imgBase64 = Base64.encodeBase64String(FileUtils.readFileToByteArray(new File(captChaPath)));
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("imgBase64", imgBase64);
			
			StringEntity userEntity = new StringEntity(jsonObject.toString(), "utf8");
			httpPost.setEntity(userEntity);
			
			HttpResponse response = httpClient.execute(httpPost);
			return jsonParser(response);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public JsonObject updateStatus(JsonArray updateLst, String productId) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start updateChkLst");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost(BASE_URL + "/restAct/paymentOnlineCheck/updateChkLst");
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			httpPost.addHeader("X-Auth-Token", this.token);
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("updateList", updateLst);
			jsonObject.addProperty("productId", productId);
			
			StringEntity userEntity = new StringEntity(jsonObject.toString(), "utf8");
			httpPost.setEntity(userEntity);
			
			HttpResponse response = httpClient.execute(httpPost);
			return jsonParser(response);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	/*public JsonObject updateChkLst(UpdateChkLstModel model) throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			LOG.debug("Start updateChkLst");
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultRequestConfig(REQUEST_CONFIG);
			
			httpClient = builder.build();
			HttpPost httpPost = new HttpPost(BASE_URL + "/restAct/paymentOnlineCheck/updateChkLst");
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			httpPost.addHeader("X-Auth-Token", this.token);
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("productId", model.getProductId());
			jsonObject.addProperty("id", model.getId());
			jsonObject.addProperty("status", model.getStatus());
			jsonObject.addProperty("sessionId", model.getSessionId());
			jsonObject.addProperty("cif", model.getCif());
			jsonObject.addProperty("loanType", model.getLoanType());
			jsonObject.addProperty("accNo", model.getAccNo());
			jsonObject.addProperty("flag", model.getFlag());
			jsonObject.addProperty("uri", model.getUri());
			
			if(model.getPaidDateTime() != null) {
				jsonObject.addProperty("paidDateTime", model.getPaidDateTime().getTime());				
			}
			
			StringEntity userEntity = new StringEntity(jsonObject.toString());
			httpPost.setEntity(userEntity);
			
			HttpResponse response = httpClient.execute(httpPost);
			return jsonParser(response);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}*/
	
	private JsonObject jsonParser(HttpResponse response) throws Exception {
		try {
			String jsonStr = jsonStr(response);
			JsonElement jsonElement =  new JsonParser().parse(jsonStr);
			return jsonElement.getAsJsonObject();
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private String jsonStr(HttpResponse response) throws Exception {
		try {
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
				   + response.getStatusLine().getStatusCode());
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
			String output, result = "";
			
			while ((output = br.readLine()) != null) {
				result = output;
			}
			br.close();
			
			return result;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}

}
