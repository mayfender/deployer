package com.may.ple.kyschkpay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class App {
	private static String token;
	
	public static void main(String[] args) {
		try {
			int i = 0;
			while(true) {
				if(i == 10) break;
				
				System.out.println("call login " + String.format("%1$tH:%1$tm:%1$tS", Calendar.getInstance().getTime()));
				login();
				
				System.out.println("call img2txt " + String.format("%1$tH:%1$tm:%1$tS", Calendar.getInstance().getTime()));
				img2txt();
				System.out.println("finished " + String.format("%1$tH:%1$tm:%1$tS", Calendar.getInstance().getTime()));
				
				Thread.sleep(1000);
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void img2txt() throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpPost httpPost = new HttpPost("http://localhost:8080/backend/restAct/tools/img2txt");
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			httpPost.addHeader("X-Auth-Token", token);
			
			HttpResponse response = httpClient.execute(httpPost);
			Map result = getResult(response);
			
			System.out.println(result);
		} catch (Exception e) {
			throw e;
		}
	}
	
	private static void login() throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpPost httpPost = new HttpPost("http://localhost:8080/backend/login");
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			
			String username = "system";
			String password = Base64.encodeBase64String("w,j[vd8iy[".getBytes());
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("username", username);
			jsonObject.addProperty("password", password);
			
			StringEntity userEntity = new StringEntity(jsonObject.toString());
			httpPost.setEntity(userEntity);
			
			HttpResponse response = httpClient.execute(httpPost);
			
			Map result = getResult(response);
			token = result.get("token").toString();			
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if(httpClient != null) httpClient.close(); 
			} catch (Exception e2) {}
		}
	}
	
	private static Map getResult(HttpResponse response) throws Exception {
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
			
			Gson gson = new GsonBuilder().create();
			Map map = gson.fromJson(result , Map.class);
			return map;
		} catch (Exception e) {
			throw e;
		}
	}
	
}