package com.may.ple.tunnel.util;

import java.io.IOException;

public class NetworkInfoUtil {
	
	public static String getPublicIp(String ipServ) throws IOException {
		
		try (java.util.Scanner s = new java.util.Scanner(new java.net.URL(ipServ).openStream(), "UTF-8").useDelimiter("\\A")) {
			return s.next();
		} catch (java.io.IOException e) {
			throw e;
		}	
	}

}
