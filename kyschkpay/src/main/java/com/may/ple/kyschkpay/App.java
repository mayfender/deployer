package com.may.ple.kyschkpay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

public class App {
	private static final Logger LOG = Logger.getLogger(App.class.getName());
	public static ManageLoginWorkerThread loginWorker;
	private static final int START_WORKING_HOUR = 5;
	private static final int END_WORKING_HOUR = 22;
	
	//--: args[0]: Product ID[proId-1,proId-2]
	public static void main(String[] args) {
		try {
			LOG.info("Start Module...");
			
			if(args == null || args.length == 0) {
				LOG.error("args can't be null");
				return;
			}
			
			List<String> prodIds = Arrays.asList(args[0].split(","));
			LOG.info("prodIds : " + prodIds);

			socketApi();
			
			LOG.info("Start LoginWorkerThread");
			loginWorker = new ManageLoginWorkerThread(prodIds);
			loginWorker.start();
			
//			LOG.info("Start LoginWorkerThread");
//			new ManageCheckPayWorkerThread(prodIds).start();
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
	public static boolean checkWorkingHour() {
		int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		return hourOfDay >= App.START_WORKING_HOUR && hourOfDay <= App.END_WORKING_HOUR;
	}
	
	private static void socketApi() {
		LOG.info("Start socket api");
		new Thread() {
			public void run() {
				ServerSocket serverSock = null;
				
				try {
					serverSock = new ServerSocket(9001);	
					
					while(true) {
						Socket socket = serverSock.accept();
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						String read;
						while((read = reader.readLine())  != null) {
							if(read.equals("SHUTDOWN")) System.exit(0);
						}
						socket.close();
					}
				} catch (Exception e) {
					LOG.error(e.toString());
				} finally {
					try {
						if(serverSock != null) serverSock.close();						
					} catch (Exception e2) {}
				}
			}
		}.start();
	}
	
}