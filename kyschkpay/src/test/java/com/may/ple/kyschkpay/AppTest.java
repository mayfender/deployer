package com.may.ple.kyschkpay;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
		try {
			App.prop = new Properties();
			Authenticator.setDefault(
					   new Authenticator() {
					      public PasswordAuthentication getPasswordAuthentication() {
					         return new PasswordAuthentication(
					        	"mayfender", "w,j[vd8iy[".toCharArray()
					         );
					      }
					   }
					);
			
			
			int i = 0;
			while(true) {
				if(i == 100) break;
				
				Proxy proxy = new Proxy(
						Proxy.Type.HTTP,
						InetSocketAddress.createUnresolved("103.86.49.81", 8080)
						);
				
//				LoginRespModel loginPage = KYSApi.getInstance().getLoginPage(proxy);
				
				/*Response res = Jsoup
						.connect("https://www.google.co.th")
						.proxy(proxy)
						.method(Method.GET).execute();*/
				
//				System.out.println(loginPage.getSessionId());
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
