package com.may.ple.kyschkpay;

public class DateUtil {
	
	public static String birthDateFormat(String str) {
		if(str.contains("/")) {
			return str;
		}
		
		String day = str.substring(0, 2);
		String month = str.substring(2, 4);
		String year = str.substring(4);
		return day + "/" + month + "/" + year;
	}

}
