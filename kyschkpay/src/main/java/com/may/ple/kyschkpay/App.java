package com.may.ple.kyschkpay;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;



/**
 * Hello world!
 *
 */
public class App {
	
	public static void main(String[] args) {
		try {
			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost httpPost = new HttpPost("http://localhost:8081/backend/login");
			httpPost.addHeader("content-type", "application/json; charset=utf8");
			
			 StringEntity userEntity = new StringEntity("{\"username\":\"sadmin\",\"password\":\"MTIz\"}");
			 httpPost.setEntity(userEntity);
			
			 HttpResponse response = httpClient.execute(httpPost);
			
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
				   + response.getStatusLine().getStatusCode());
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
		
			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				System.out.println(output);
			}
		
			httpClient.getConnectionManager().shutdown();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
