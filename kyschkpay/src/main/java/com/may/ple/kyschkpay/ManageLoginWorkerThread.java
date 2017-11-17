package com.may.ple.kyschkpay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ManageLoginWorkerThread extends Thread {
	private static final Logger LOG = Logger.getLogger(ManageLoginWorkerThread.class.getName());
	private static List<UpdateChkLstModel> loginSuccessList = new ArrayList<>();
	private static List<UpdateChkLstModel> loginFailList = new ArrayList<>();
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	private static final int POOL_SIZE = 20;
	private List<String> prodIds;
	
	public ManageLoginWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}

	@Override
	public void run() {
		try {
			DMSApi dmsApi = DMSApi.getInstance();
			ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(POOL_SIZE);
			String birthDateColumnName;
			String idCardNoColumnName;
			JsonObject loginChkList;
			JsonElement checkList;
			JsonArray jsonArray;
			Runnable worker;
			int currentPage = 1;
			int itemsPerPage = 100;
			
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
					LOG.info("Start for product id: " + prodId);
					
					loginSuccessList.clear();
					loginFailList.clear();
					
					dmsApi.initData(prodId);
					
					loginChkList = dmsApi.getChkList(prodId, currentPage, itemsPerPage, StatusConstant.PENDING.getStatus());
					int totalItems = loginChkList.get("totalItems").getAsInt();
					int totalPages = (int)Math.ceil((double)totalItems / (double)itemsPerPage);
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {							
							loginChkList = dmsApi.getChkList(prodId, currentPage, itemsPerPage, StatusConstant.PENDING.getStatus());
						}
						
						idCardNoColumnName = loginChkList.get("idCardNoColumnName").getAsString();
						birthDateColumnName = loginChkList.get("birthDateColumnName").getAsString();
						checkList = loginChkList.get("checkList");
						
						if(checkList == null) continue;
						
						jsonArray = checkList.getAsJsonArray();
						
						for (JsonElement el : jsonArray) {
							while(executor.getQueue().size() == POOL_SIZE) {
								LOG.info("Pool size full : " + executor.getQueue().size());						
								Thread.sleep(5000);
							}
							
							worker = new LoginWorkerThread(el, idCardNoColumnName, birthDateColumnName);
							executor.execute(worker);
						}
					}
					
					while(executor.getQueue().size() != 0){
						LOG.debug("Wait pool size = 0");
						Thread.sleep(1000);
					}
					
					updateLoginStatus();
					
					LOG.info("Finished for product id: " + prodId);
				}
			}
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
	private void updateLoginStatus() throws Exception {
		try {
			LOG.info("Update login success");
			String jsonSuccess = new Gson().toJson(loginSuccessList);
			DMSApi.getInstance().updateLoginSuccess(jsonSuccess, StatusConstant.LOGIN_SUCCESS.getStatus());
			
			LOG.info("Update login fail");
			String jsonFail = new Gson().toJson(loginFailList);
			DMSApi.getInstance().updateLoginSuccess(jsonFail, StatusConstant.LOGIN_FAIL.getStatus());
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public synchronized static void addToLoginSuccessList(UpdateChkLstModel model) {
		loginSuccessList.add(model);
	}
	public synchronized static void addToLoginFailList(UpdateChkLstModel model) {
		loginFailList.add(model);
	}
	
}
