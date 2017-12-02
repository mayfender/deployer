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
		String contractNoColumnName;
		JsonElement checkList;
		JsonArray jsonArray;
		JsonObject chkList;
		int numOfEachProxy;
		int currentPage;
		int proxySize = 0;
		int proxyIndex;
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
				proxyIndex = 0;
				
//				proxiesIndex.add("NOPROXY");
				proxiesIndex.add("13.114.101.65:8080");
//				proxiesIndex.add("us-wa.proxymesh.com:31280");
				
				for (String prxIndex : proxiesIndex) {
					proxies.put(prxIndex, new ArrayList<ChkPayWorkerModel>());
				}
				
				for (String prodId : prodIds) {
					LOG.info("Start for product id: " + prodId);
					
					currentPage = 1;
					chkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
					if(chkList == null) break;
					
					totalItems = chkList.get("totalItems").getAsInt();
					totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					numOfEachProxy = totalItems / proxiesIndex.size();
					
					LOG.debug("numOfEachProxy: " + numOfEachProxy);
					LOG.debug("totalItems: " + totalItems);
					if(totalItems == 0) continue;
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {							
							chkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
							if(chkList == null) break;
						}
						
						checkList = chkList.get("checkList");
						contractNoColumnName = chkList.get("contractNoColumnName").getAsString();
						
						if(checkList == null) continue;
						
						jsonArray = checkList.getAsJsonArray();
						
						for (JsonElement el : jsonArray) {
							proxies.get(proxiesIndex.get(proxyIndex)).add(new ChkPayWorkerModel(prodId, el, contractNoColumnName));
							proxySize++;
							
							if((proxyIndex + 1) < proxiesIndex.size()) {
								if(proxySize == numOfEachProxy) {
									LOG.debug("proxyIndex: " + proxyIndex);
									LOG.debug("proxySize: " + proxySize);
									
									executor.execute(new ChkPayProxyWorker(
											proxiesIndex.get(proxyIndex), 
											proxies.get(proxiesIndex.get(proxyIndex))
											));
									
									proxyIndex++;
									proxySize = 0;
								}
							}
						}
						
						LOG.debug("Execute Last Worker Group");
						executor.execute(new ChkPayProxyWorker(
								proxiesIndex.get(proxyIndex), 
								proxies.get(proxiesIndex.get(proxyIndex))
								));
						
					}
					
					LOG.info("Finished for product id: " + prodId);
				}
				
				Thread.sleep(5000);
				while(executor.getActiveCount() != 0){
					LOG.debug("=============: Manager Worker active count : " + executor.getActiveCount());
					Thread.sleep(1000);
				}
				
				LOG.info("Finished all product");
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
