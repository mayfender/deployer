package com.may.ple.kyschkpay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
		Set<Entry<String, List<ChkPayWorkerModel>>> proxySet;
		Map<String, List<ChkPayWorkerModel>> proxies;
		String proxiesIndexStr = "NOPROXY";
		boolean isClear = Boolean.FALSE;
		String contractNoColumnName;
		JsonElement checkList;
		JsonArray jsonArray;
		JsonObject chkList;
		JsonObject data;
		int currentPage;
		int totalItems;
		int totalPages;
		
		while(true) {
			try {
				if(!App.checkWorkingHour()) {
					if(!isClear) {
						LOG.info("Clear status to login");
						for (String prodId : prodIds) {
							dmsApi.clearStatusChkLst(prodId);
						}
						isClear = Boolean.TRUE;
					}
					
					LOG.info("Sleep 30 min");
					Thread.sleep(1800000);
					continue;
				}
				
				isClear = Boolean.FALSE;
				if(!dmsApi.login(USERNAME, PASSWORD)) {
					LOG.warn("May be server is down.");
					Thread.sleep(30000);
					continue;
				}
				
				for (String prodId : prodIds) {
					LOG.info("Start for product id: " + prodId);
					
					proxies = new HashMap<>();
					currentPage = 1;
					chkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
					if(chkList == null) break;
					
					totalItems = chkList.get("totalItems").getAsInt();
					totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					
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
							data = el.getAsJsonObject();
							
							if(data.get("sys_proxy").isJsonNull()) {
								proxiesIndexStr = "NOPROXY";
							} else {
								proxiesIndexStr = data.get("sys_proxy").getAsString();
							}
							if(proxies.get(proxiesIndexStr) == null) {
								proxies.put(proxiesIndexStr, new ArrayList<ChkPayWorkerModel>());
							}
							
							proxies.get(proxiesIndexStr).add(new ChkPayWorkerModel(prodId, el, contractNoColumnName));
						}	
					}
					
					proxySet = proxies.entrySet();
					for (Entry<String, List<ChkPayWorkerModel>> proxyEnt : proxySet) {
						LOG.debug("Execute " + proxyEnt.getKey() + " size: " + proxyEnt.getValue().size());
						executor.execute(new ChkPayProxyWorker(proxyEnt.getKey(), proxyEnt.getValue()));
					}
					
					LOG.info("Finished for product id: " + prodId);
					Thread.sleep(5000);
					while(executor.getActiveCount() != 0){
						LOG.debug("=============: Manager Worker active count : " + executor.getActiveCount());
						Thread.sleep(1000);
					}
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
