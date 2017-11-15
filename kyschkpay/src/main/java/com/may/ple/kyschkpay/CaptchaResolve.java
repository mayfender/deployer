package com.may.ple.kyschkpay;

import org.apache.log4j.Logger;

public class CaptchaResolve {
	private static final Logger LOG = Logger.getLogger(CaptchaResolve.class.getName());
	
	public static String tesseract(String imgPath) throws Exception {
		try {			
			String baseDir = "D:/python_captcha";
			String tesseractPath = "C:/Program Files (x86)/Tesseract-OCR";
			String pythonPath = "C:/Users/sarawuti/AppData/Local/Programs/Python/Python36-32";
			
			return CaptchaUtil.tesseract(imgPath, baseDir, tesseractPath, pythonPath);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}

}
