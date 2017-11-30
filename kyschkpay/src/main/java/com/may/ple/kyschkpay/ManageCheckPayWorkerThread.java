package com.may.ple.kyschkpay;

import java.net.Proxy;
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
	private static final int POOL_SIZE = 1;
	private static final int LIMITED_UPDATE_SIZE = 1000;
	private static final int ITEMS_PER_PAGE = 1000;
	private List<String> prodIds;
	
	public ManageCheckPayWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}

	@Override
	public void run() {
		DMSApi dmsApi = DMSApi.getInstance();
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(POOL_SIZE);
		String contractNoColumnName;
		JsonObject chkList;
		JsonElement checkList;
		JsonArray jsonArray;
		Runnable worker;
		int currentPage;
		
		/*Proxy proxy = new Proxy(
				Proxy.Type.HTTP,                                      
				InetSocketAddress.createUnresolved("180.183.112.220", 8080)
		);		*/
		Proxy proxy = null;
		
		while(true) {
			try {
				if(!dmsApi.login(USERNAME, PASSWORD)) {
					LOG.warn("May be server is down.");
					Thread.sleep(30000);
					continue;
				}
				
				for (String prodId : prodIds) {
					LOG.info("Start for product id: " + prodId);
					
					chkPayList.clear();
					currentPage = 1;
					
					chkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
					if(chkList == null) break;
					
					int totalItems = chkList.get("totalItems").getAsInt();
					int totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					
					LOG.debug("totalItems: " + totalItems);
					if(totalItems == 0) continue;
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {							
							chkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
							if(chkList == null) break;
						}
						
						LOG.debug("chkPayList size: " + chkPayList.size());
						checkList = chkList.get("checkList");
						contractNoColumnName = chkList.get("contractNoColumnName").getAsString();
						
						if(checkList == null) continue;
						
						jsonArray = checkList.getAsJsonArray();
						
						for (JsonElement el : jsonArray) {
							worker = new ChkPayWorkerThread(proxy, prodId, el, contractNoColumnName);
							executor.execute(worker);
						}
					}
					
					Thread.sleep(10000);
					while(executor.getActiveCount() != 0){
						LOG.debug("=============: Worker active count : " + executor.getActiveCount());
						Thread.sleep(1000);
					}
					
					LOG.debug("chkPayList size: " + chkPayList.size());
					updateChkPayStatus(prodId);
					
					LOG.info("Finished for product id: " + prodId);
				}
			} catch (Exception e) {
				LOG.error(e.toString(), e);
			} finally {
				try {
					//--: Sleep 10 minutes
					Thread.sleep(600000);											
				} catch (Exception e2) {}
			}
		}
	}
	
	private void updateChkPayStatus(String productId) throws Exception {
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
				obj.addProperty("createdDateTime", modelLst.getCreatedDateTime().getTime());
				obj.addProperty("html", modelLst.getHtml());
				obj.addProperty("contractNo", modelLst.getContractNo());
				
				array.add(obj);
			}
			
			LOG.info("Call updateLoginStatus");
			DMSApi.getInstance().updateStatus(array, productId);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public synchronized void addToChkPayList(UpdateChkLstModel model, String productId) {
		try {
			chkPayList.add(model);
			LOG.debug("chkPayList size: " + chkPayList.size());
			
			if(chkPayList.size() == LIMITED_UPDATE_SIZE) {
				LOG.info("Call updateChkPayStatus");
				updateChkPayStatus(productId);
				chkPayList.clear();
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
}
