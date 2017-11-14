package com.may.ple.kyschkpay;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ManageCheckPayWorkerThread extends Thread {
	private static final Logger LOG = Logger.getLogger(ManageCheckPayWorkerThread.class.getName());
	private List<String> prodIds;
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	
	public ManageCheckPayWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}

	@Override
	public void run() {
		try {
			int poolSize = 20;
			DMSApi dmsApi = new DMSApi();
			ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(poolSize);
			String birthDateColumnName;
			String idCardNoColumnName;
			JsonObject loginChkList;
			JsonElement checkList;
			JsonArray jsonArray;
			Runnable worker;
			int currentPage = 1;
			int itemsPerPage = 50;
			
			while(true) {
				if(!App.checkWorkingHour()) {
					LOG.info("Sleep 30 min");
					Thread.sleep(1800000);
					continue;
				}
				
				if(!dmsApi.login(USERNAME, PASSWORD)) {
					LOG.warn("May be server is down.");
					Thread.sleep(30000);
					continue;
				}
				
				for (String prodId : prodIds) {
					while(executor.getQueue().size() == poolSize) {
						LOG.info("Pool size full : " + executor.getQueue().size());						
						Thread.sleep(30000);
					}
					
					dmsApi.initData(prodId);
					loginChkList = dmsApi.getChkList(prodId, currentPage, itemsPerPage, 1);
					idCardNoColumnName = loginChkList.get("idCardNoColumnName").getAsString();
					birthDateColumnName = loginChkList.get("birthDateColumnName").getAsString();
					checkList = loginChkList.get("checkList");
					
					if(checkList == null) continue;
					
					jsonArray = checkList.getAsJsonArray();
					
					for (JsonElement el : jsonArray) {
						worker = new WorkerThread(el, idCardNoColumnName, birthDateColumnName, prodId);
						executor.execute(worker);
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
}
