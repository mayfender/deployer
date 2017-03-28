package com.may.ple.deployer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Locale;

public class Deployer {
	private static String separator = File.separator;
	private static String tomcatHome;
	private static String warFile;
	
	public static void main(String[] args) {
		try {
			
			log("Check args");
			if(args != null) {
				log("args size: " + args.length);
				tomcatHome = args[0];
				warFile = args[1];
				log(tomcatHome);
				log(warFile);
			} else {
				log("Not found args");
			}
			
			System.out.println("=============: Start Deploy Process :==============");
			log("=============: Start Deploy Process :==============");
			
			System.out.println("Call windows");
			windows();
			
			System.out.println("=============: End Deploy Process :==============");
			log("=============: End Deploy Process :==============");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*private static void linux() throws Exception {
		log("Linux SSH");
	}*/
	
	private static void windows() throws Exception {
		try {
			log("Windows CMD");
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
			System.out.println("Remove old version");
			log("Remove old version files");
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
			log("Prepare new version file");
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
			log("Start tomcat");
			String[] cmd = { "cmd", "/c", "start", "startup.bat"};
			ProcessBuilder procBuilder = new ProcessBuilder(cmd);
			procBuilder.directory(new File(tomcatHome + separator + "bin"));
			procBuilder.start();
	    	
			log("Start finished");			
		} catch (Exception e) {
			log(e.toString());
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
            	System.out.println(line);
            	log(line);
            }
		} catch (Exception e) {
			System.err.println(e.toString());
		} finally {
			try { if(reader != null) reader.close(); } catch (Exception e2) {}
		}
	}
	
	private static void log(String msg) {
		PrintWriter writer = null;
		
		try {
			String file = "C:/Users/mayfender/Desktop/Deployer/test.txt";
			writer = new PrintWriter(new FileOutputStream(new File(file), true));
			writer.println(String.format(Locale.ENGLISH, "%1$tH:%1$tM:%1$tS ----- %2$s", new Date(), msg));			
		} catch (Exception e) {
			writer.println(e.toString());
		} finally {
			if(writer != null) writer.close();
		}
	}

}
