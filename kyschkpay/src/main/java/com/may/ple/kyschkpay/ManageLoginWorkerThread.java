package com.may.ple.kyschkpay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ManageLoginWorkerThread extends Thread {
	private static final Logger LOG = Logger.getLogger(ManageLoginWorkerThread.class.getName());
	private static List<UpdateChkLstModel> loginList = new ArrayList<>();
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	private static final int POOL_SIZE = 20;
	private static final int LIMITED_UPDATE_SIZE = 1000;
	private static final int ITEMS_PER_PAGE = 1000;
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
			int currentPage;
			
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
					
					loginList.clear();
					currentPage = 1;
					
					loginChkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "LOGIN");
					int totalItems = loginChkList.get("totalItems").getAsInt();
					int totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					
					LOG.debug("totalItems: " + totalItems);
					if(totalItems == 0) continue;
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {							
							loginChkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "LOGIN");
						}
						
						LOG.debug("loginSuccessList size: " + loginList.size());						
						idCardNoColumnName = loginChkList.get("idCardNoColumnName").getAsString();
						birthDateColumnName = loginChkList.get("birthDateColumnName").getAsString();
						checkList = loginChkList.get("checkList");
						
						if(checkList == null) continue;
						
						jsonArray = checkList.getAsJsonArray();
						
						for (JsonElement el : jsonArray) {
							worker = new LoginWorkerThread(prodId, el, idCardNoColumnName, birthDateColumnName);
							executor.execute(worker);
						}
					}
					
					Thread.sleep(10000);
					while(executor.getActiveCount() != 0){
						LOG.debug("=============: Worker active count : " + executor.getActiveCount());
						Thread.sleep(1000);
					}
					
					LOG.debug("loginList size: " + loginList.size());
					updateLoginStatus(prodId);
					
					LOG.info("Finished for product id: " + prodId);
				}
				
				//--: Sleep 1 minutes
				Thread.sleep(60000);
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	private void updateLoginStatus(String productId) throws Exception {
		try {
			if(loginList.size() == 0) return;
			
			LOG.info("Update login success");
			JsonArray array = new JsonArray();
			JsonObject obj;
			
			for (UpdateChkLstModel modelLst : loginList) {
				obj = new JsonObject();
				obj.addProperty("id", modelLst.getId());
				obj.addProperty("accNo", modelLst.getAccNo());
				obj.addProperty("cif", modelLst.getCif());
				obj.addProperty("errMsg", modelLst.getErrMsg());
				obj.addProperty("flag", modelLst.getFlag());
				obj.addProperty("loanType", modelLst.getLoanType());
				obj.addProperty("sessionId", modelLst.getSessionId());
				obj.addProperty("status", modelLst.getStatus());
				obj.addProperty("uri", modelLst.getUri());
				array.add(obj);
			}
			
			LOG.info("Call updateLoginStatus");
			DMSApi.getInstance().updateStatus(array, productId);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public synchronized void addToLoginList(UpdateChkLstModel model, String productId) {
		try {
			loginList.add(model);
			LOG.debug("loginList size: " + loginList.size());
			
			if(loginList.size() == LIMITED_UPDATE_SIZE) {
				LOG.info("Call updateLoginStatus");
				updateLoginStatus(productId);
				loginList.clear();
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
}
