package com.may.ple.deployer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;

import com.may.ple.deployer.utils.EmailUtil;
import com.may.ple.deployer.utils.LogUtil;

public class Deployer {
	private static String separator = File.separator;
	private static String tomcatHome;
	private static String warFile;
	private static String comCode;
	
	public static void main(String[] args) {
		try {
			LogUtil.log("Check args");
			if(args != null) {
				LogUtil.log("args size: " + args.length);
				tomcatHome = args[0];
				warFile = args[1].replace("/", "\\");
				comCode = args[2];
				LogUtil.log(tomcatHome);
				LogUtil.log(warFile);
			} else {
				LogUtil.log("Not found args");
			}
			
			LogUtil.log("=============: Start Deploy Process :==============");
			windows();
			
			LogUtil.log("=============: End Deploy Process :==============");
			sendLog();
			
			new File(LogUtil.logFilePath).delete();		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*private static void linux() throws Exception {
		log("Linux SSH");
	}*/
	
	private static void windows() throws Exception {
		try {
			LogUtil.log("Windows CMD");
			Thread.sleep(30000);
			
			removeOldFile();
			
			Thread.sleep(3000);
			
			copyFile();
			
			Thread.sleep(3000);
			
			start();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	private static void removeOldFile() throws Exception {
		InputStream in = null;
		
		try {			
			LogUtil.log("Remove old version files");
			StringBuilder commands = new StringBuilder();
			commands.append("cmd /c cd " + tomcatHome + separator + "webapps ");
			commands.append("&& del backend.war ");
			commands.append("& rm -rf backend ");
			commands.append("& cd ../temp ");
			commands.append("&& rm -rf backend ");
			commands.append("& cd ../work/Catalina/localhost ");
			commands.append("&& rm -rf backend ");
			
			Process proc = Runtime.getRuntime().exec(commands.toString());
			in = proc.getInputStream();
			print(in);
		} catch (Exception e) {
			throw e;
		} finally {
			try { if(in != null) in.close(); } catch (Exception e2) {}			
		}
	}
	
	private static void copyFile() throws Exception {
		InputStream in = null;
		try {
			LogUtil.log("Prepare new version file");
			StringBuilder commands = new StringBuilder();
			commands.append("cmd /c cd " + tomcatHome + separator + "webapps ");
			commands.append("&& copy /-y " + warFile + " backend.war");
			Process proc = Runtime.getRuntime().exec(commands.toString());
			in = proc.getInputStream();
			print(in);
		} catch (Exception e) {
			throw e;
		} finally {
			try { if(in != null) in.close(); } catch (Exception e2) {}			
		}
	}
	
	private static void start() throws Exception {
		InputStream in = null;
		try {
			LogUtil.log("Start tomcat");
			String[] cmd = { "cmd", "/c", "start", "startup.bat"};
			ProcessBuilder procBuilder = new ProcessBuilder(cmd);
			procBuilder.directory(new File(tomcatHome + separator + "bin"));
			procBuilder.start();
	    	
			LogUtil.log("Start finished");			
		} catch (Exception e) {
			LogUtil.log(e.toString());
			throw e;
		} finally {
			try { if(in != null) in.close(); } catch (Exception e2) {}			
		}
	}
	
	private static void print(InputStream in) {
		BufferedReader reader = null;
		String line = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(in));
            
            while ((line = reader.readLine()) != null) {
            	LogUtil.log(line);
            }
		} catch (Exception e) {
			LogUtil.log(e.toString());
		} finally {
			try { if(reader != null) reader.close(); } catch (Exception e2) {}
		}
	}
	
	private static void sendLog() {
		try {
			byte[] bytes = Files.readAllBytes(new File(LogUtil.logFilePath).toPath());
			EmailUtil.sendSimple(comCode + "_SystemSent", new String(bytes,"UTF-8").toString());
		} catch (Exception e) {
			LogUtil.log(e.toString());
		}
	}

}