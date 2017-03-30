package com.may.ple.deployer.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.CodeSource;
import java.util.Date;
import java.util.Locale;

import com.may.ple.deployer.Deployer;

public class LogUtil {
	public static final String logFilePath;
	public static final String logFile;
	static {
		logFile = "deployer.log";
		logFilePath = getLogPath();
	}
	
	public static void log(String msg) {
		PrintWriter writer = null;
		
		try {			
			writer = new PrintWriter(new FileOutputStream(new File(logFilePath), true));
			writer.println(String.format(Locale.ENGLISH, "%1$tH:%1$tM:%1$tS ----- %2$s", new Date(), msg));			
		} catch (Exception e) {
			writer.println(e.toString());
		} finally {
			if(writer != null) writer.close();
		}
	}
	
	private static String getLogPath() {
		try {
			CodeSource codeSource = Deployer.class.getProtectionDomain().getCodeSource();
			File jarFile = new File(codeSource.getLocation().toURI().getPath());
			String jarDir = jarFile.getParentFile().getPath();
			return jarDir + File.separator + logFile;			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
