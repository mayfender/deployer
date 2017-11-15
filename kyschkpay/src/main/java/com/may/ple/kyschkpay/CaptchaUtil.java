package com.may.ple.kyschkpay;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.log4j.Logger;

public class CaptchaUtil {
	private static final Logger LOG = Logger.getLogger(CaptchaUtil.class.getName());
	
	public static String tesseract(String imgBase64, String baseDir, String tesseractPath, String pythonPath) throws Exception {
		Process process = null;
		BufferedReader reader = null;
		
		try {
			String slash = File.separator;
			String tesseractPathEndSlash = tesseractPath + slash;
			String[] cmd = { 
							pythonPath + slash + "python", 
					        "parse_captcha.py", 
					        imgBase64,
					        tesseractPathEndSlash 
					        };
	    	ProcessBuilder pb = new ProcessBuilder(cmd);
	    	Map<String, String> env = pb.environment();
	    	env.put("TESSDATA_PREFIX", tesseractPathEndSlash + "tessdata");
	    	pb.directory(new File(baseDir));
	    	process = pb.start();
	    	
	    	reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    	String read;
	    	while((read = reader.readLine()) != null) {
	    		if(!read.contains("captcha_txt")) {
	    			LOG.info(read);
	    			continue;
	    		}
	    		
	    		return read.split(":")[1].trim();
	    	}
	    	return "";
		} catch (Exception e) {
			throw e;
		} finally {
			try { if(reader != null) reader.close(); } catch (Exception e2) {}
			try { if(process != null) process.destroy(); } catch (Exception e2) {}
		}
	}

}