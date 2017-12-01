package com.may.ple.kyschkpay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private static final int POOL_SIZE = 100;
	private static final int ITEMS_PER_PAGE = 1000;
	private List<String> prodIds;
	
	public ManageCheckPayWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}

	@Override
	public void run() {
		DMSApi dmsApi = DMSApi.getInstance();
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(POOL_SIZE);
		Map<String, List<ChkPayWorkerModel>> proxies = new HashMap<>();
		List<String> proxiesIndex = new ArrayList<>();
		ManageProxyModel manageProxyModel;
		String contractNoColumnName;
		JsonElement checkList;
		JsonArray jsonArray;
		JsonObject chkList;
		int numOfEachProxy;
		int currentPage;
		int totalItems;
		int totalPages;
		
		while(true) {
			try {
				if(!dmsApi.login(USERNAME, PASSWORD)) {
					LOG.warn("May be server is down.");
					Thread.sleep(30000);
					continue;
				}
				
				//--: Initial worker
				manageProxyModel = new ManageProxyModel();
				
				proxiesIndex.add("NOPROXY");
				/*proxiesIndex.add("180.183.112.220:8080");
				proxiesIndex.add("180.183.112.221:8080");
				proxiesIndex.add("180.183.112.222:8080");*/
				
				for (String prxIndex : proxiesIndex) {
					proxies.put(prxIndex, new ArrayList<ChkPayWorkerModel>());
				}
				
				for (String prodId : prodIds) {
					LOG.info("Start for product id: " + prodId);
					
					chkPayList.clear();
					currentPage = 1;
					
					chkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
					if(chkList == null) break;
					
					totalItems = chkList.get("totalItems").getAsInt();
					totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					numOfEachProxy = totalItems / proxiesIndex.size();
					
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
							proxies.get(proxiesIndex.get(manageProxyModel.proxyIndex)).add(new ChkPayWorkerModel(prodId, el, contractNoColumnName));
							manageProxyModel.proxySize++;
							
							if((proxiesIndex.size() != manageProxyModel.proxyIndex) && (numOfEachProxy == manageProxyModel.proxySize)) {
								LOG.debug("proxyIndex: " + manageProxyModel.proxyIndex);
								LOG.debug("proxySize: " + manageProxyModel.proxySize);
								
								executor.execute(new ChkPayWorkerThreadTest(
										proxiesIndex.get(manageProxyModel.proxyIndex), 
										proxies.get(proxiesIndex.get(manageProxyModel.proxyIndex))
								));
								
								manageProxyModel.proxyIndex++;
								manageProxyModel.proxySize = 0;
							}
						}
					}
					
					LOG.info("Finished for product id: " + prodId);
				}
				
				Thread.sleep(10000);
				while(executor.getActiveCount() != 0){
					LOG.debug("=============: Worker active count : " + executor.getActiveCount());
					Thread.sleep(1000);
				}
				
				LOG.info("Finished round");
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
	
}
