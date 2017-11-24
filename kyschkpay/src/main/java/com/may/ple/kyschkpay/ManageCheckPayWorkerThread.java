package com.may.ple.kyschkpay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ManageCheckPayWorkerThread extends Thread {
	private static final Logger LOG = Logger.getLogger(ManageCheckPayWorkerThread.class.getName());
	private static List<UpdateChkLstModel> chkPayList = new ArrayList<>();
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	private static final int POOL_SIZE = 1000;
	private static final int LIMITED_UPDATE_SIZE = 1000;
	private static final int ITEMS_PER_PAGE = 1000;
	private List<String> prodIds;
	
	public ManageCheckPayWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}

	@Override
	public void run() {
		try {
			DMSApi dmsApi = DMSApi.getInstance();
			ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(POOL_SIZE);
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
					
					chkPayList.clear();
					currentPage = 1;
					
					loginChkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
					int totalItems = loginChkList.get("totalItems").getAsInt();
					int totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					if(totalItems == 0) continue;
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {							
							loginChkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
						}
						
						LOG.debug("chkPayList size: " + chkPayList.size());
						checkList = loginChkList.get("checkList");
						
						if(checkList == null) continue;
						
						jsonArray = checkList.getAsJsonArray();
						
						for (JsonElement el : jsonArray) {
							worker = new ChkPayWorkerThread(prodId, el);
							executor.execute(worker);
						}
					}
					
					while(executor.getActiveCount() != 0){
						LOG.debug("=============: Worker active count : " + executor.getActiveCount());
						Thread.sleep(1000);
					}
					
					LOG.debug("loginSuccessList size: " + chkPayList.size());
					updateChakPayStatus(prodId);
					
					LOG.info("Finished for product id: " + prodId);
				}
				
				//--: Sleep 10 minutes
				Thread.sleep(600000);
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	private void updateChakPayStatus(String productId) throws Exception {
		try {
			if(chkPayList.size() == 0) return;
			
			LOG.info("Update check payment");
			JsonArray array = new JsonArray();
			JsonObject obj;
			
			for (UpdateChkLstModel modelLst : chkPayList) {
				obj = new JsonObject();
				obj.addProperty("id", modelLst.getId());
				obj.addProperty("status", modelLst.getStatus());
				obj.addProperty("errMsg", modelLst.getErrMsg());
				
				if(modelLst.getLastPayDate() != null) {
					obj.addProperty("lastPayDate", modelLst.getLastPayDate().getTime());					
				}
				obj.addProperty("lastPayAmount", modelLst.getLastPayAmount());
				obj.addProperty("totalPayInstallment", modelLst.getTotalPayInstallment());
				obj.addProperty("preBalance", modelLst.getPreBalance());
				
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
			chkPayList.add(model);
			LOG.debug("loginList size: " + chkPayList.size());
			
			if(chkPayList.size() == LIMITED_UPDATE_SIZE) {
				LOG.info("Call updateLoginStatus");
				updateChakPayStatus(productId);
				chkPayList.clear();
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
}
