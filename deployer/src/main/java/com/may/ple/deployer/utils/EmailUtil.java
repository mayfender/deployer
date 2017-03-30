package com.may.ple.deployer.utils;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

public class EmailUtil {
	
	public static void sendSimple(String subject, String msg) {
		try {
		    Email email = new SimpleEmail();
		    email.setHostName("smtp.googlemail.com");
		    email.setSmtpPort(465);
		    email.setAuthenticator(new DefaultAuthenticator("mayfender.debt@gmail.com", "19042528"));
		    email.setSSLOnConnect(true);
		    email.setFrom("mayfender.debt@gmail.com");
		    email.setSubject(subject);
		    email.setMsg(msg);
		    email.addTo("mayfender.debt@gmail.com");
		    email.send();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
