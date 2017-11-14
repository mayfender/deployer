package com.may.ple.kyschkpay;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

public class LoginWorkerThread extends Thread {
	private static final Logger LOG = Logger.getLogger(LoginWorkerThread.class.getName());
	private List<String> prodIds;
	
	public LoginWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}

	@Override
	public void run() {
		try {
			int poolSize = 500;
			DMSApi dmsApi = new DMSApi();
			ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(poolSize);
			
			while(true) {
				for (String prodId : prodIds) {
					while(executor.getQueue().size() == poolSize) {
						LOG.info("Pool size full : " + executor.getQueue().size());						
						Thread.sleep(30000);
					}
					
					isSuccess = dmsApi.login(USERNAME, PASSWORD);
					if(!isSuccess) {
						LOG.warn("May be server is down.");
						Thread.sleep(30000);
						continue;
					}
					
					
					
					
					
					
				}
			}
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
}
