package com.may.ple.kyschkpay;

import org.jsoup.Jsoup;


/**
 * Hello world!
 *
 */
public class App {
	
	public static void main(String[] args) {
		try {
			/*String url = "http://localhost:8081/backend/tools/img2txt";
			String username = "sadmin";
			String password = "123";*/
			
			/*String json = Jsoup
					.connect(url)
					.data("username", username)
					.data("password", new String(Base64.encodeBase64(password.getBytes())))
					.method(Method.POST)
					.header("X-Auth-Token", "eyJhbGciOiJIUzUxMiJ9.eyJleHAiOjE1MDg1MzM1MjcsInN1YiI6InNhZG1pbiIsImF1ZGllbmNlIjoid2ViIiwiYXV0aG9yaXRpZXMiOlt7ImF1dGhvcml0eSI6IlJPTEVfU1VQRVJBRE1JTiJ9XSwiY3JlYXRlZCI6MTUwODQ5MDMyNzY3M30.xydmpquHtP1maLt5M_1rkQ7SEuoDGUKkgDiBIHBPQZf9TeVNGhqF_nBvFDuub0i7bFdsQrPqDNHxfhaJo_NIow")
					.header("X-Requested-With", "XMLHttpRequest")
					.execute()
					.body();
			System.out.println(json);*/			
			
			String url = "http://localhost:8081/backend/restAct/tools/img2txt";
			Jsoup.connect(url).post();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
