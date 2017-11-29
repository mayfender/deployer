package com.may.ple.kyschkpay;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

public class App {
	private static final Logger LOG = Logger.getLogger(App.class.getName());
	public static ManageCheckPayWorkerThread chkPayWorker;
	public static ManageLoginWorkerThread loginWorker;
	private static final int START_WORKING_HOUR = 5;
	private static final int END_WORKING_HOUR = 20;
	
	//--: args[0]: Product ID[proId-1,proId-2]
	public static void main(String[] args) {
		try {
			LOG.info("Start Module...");
			System.setProperty("java.net.preferIPv4Stack", "true");
			
			if(args == null || args.length == 0) {
				LOG.error("args can't be null");
				return;
			}
			
			List<String> prodIds = Arrays.asList(args[0].split(","));
			LOG.info("prodIds : " + prodIds);
			
			LOG.info("Start ManageLoginWorkerThread");
			loginWorker = new ManageLoginWorkerThread(prodIds);
			loginWorker.start();
			
			LOG.info("Start ManageCheckPayWorkerThread");
			chkPayWorker = new ManageCheckPayWorkerThread(prodIds);
			chkPayWorker.start();
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
	public static boolean checkWorkingHour() {
		int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		return hourOfDay >= App.START_WORKING_HOUR && hourOfDay <= App.END_WORKING_HOUR;
	}
	
}