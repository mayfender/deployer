package com.may.ple.kyschkpay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ManageCheckPayWorkerThread extends Thread {
	private static final Logger LOG = Logger.getLogger(ManageCheckPayWorkerThread.class.getName());
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	private static final int ITEMS_PER_PAGE = 1000;
	private List<String> prodIds;
	
	public ManageCheckPayWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}

	@Override
	public void run() {
		int chkPoolSize = Integer.parseInt(App.prop.getProperty("pool_size_checkpay"));
		DMSApi dmsApi = DMSApi.getInstance();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 50, 180, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		Map<String, List<ChkPayWorkerModel>> proxies = new HashMap<>();
		Map<String, ThreadPoolExecutor> chkPayPools = new HashMap<>();
		Set<Entry<String, List<ChkPayWorkerModel>>> proxySet;
		String proxiesIndexStr = "NOPROXY";
		boolean isClear = Boolean.FALSE;
		String contractNoColumnName;
		JsonElement checkList;
		String token = null;
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
						token = dmsApi.login(USERNAME, PASSWORD);
						
						for (String prodId : prodIds) {
							dmsApi.clearStatusChkLst(token, prodId, USERNAME, PASSWORD);
						}
						isClear = Boolean.TRUE;
					}
					
					LOG.info("Sleep 30 min");
					Thread.sleep(1800000);
					continue;
				}
				
				isClear = Boolean.FALSE;
				
				if(StringUtils.isBlank(token = dmsApi.login(USERNAME, PASSWORD))) {
					LOG.warn("May be server is down.");
					Thread.sleep(30000);
					continue;
				}
				
				LOG.info("chkPayPools: " + chkPayPools.size());
				if(chkPayPools.size() > 0) {
					executor.setCorePoolSize(chkPayPools.size());
					executor.setMaximumPoolSize(chkPayPools.size());
				}
				
				for (String prodId : prodIds) {
					LOG.info("Start for product id: " + prodId);
					
					currentPage = 1;
					LOG.info("Call getChkList");
					chkList = dmsApi.getChkList(token, prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
					if(chkList == null) {
						LOG.info("Not found loginChkList");
						continue;
					}
					
					totalItems = chkList.get("totalItems").getAsInt();
					totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					
					LOG.info("totalItems: " + totalItems);
					if(totalItems == 0) {
						LOG.info("Wait 30 sec");
						Thread.sleep(30000);
						continue;
					}
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {
							Thread.sleep(500);
							LOG.info("Call getChkList");
							chkList = dmsApi.getChkList(token, prodId, currentPage, ITEMS_PER_PAGE, "CHKPAY");
							if(chkList == null) break;
						}
						
						checkList = chkList.get("checkList");
						contractNoColumnName = chkList.get("contractNoColumnName").getAsString();
						
						if(checkList == null) continue;
						
						jsonArray = checkList.getAsJsonArray();
						
						for (JsonElement el : jsonArray) {
							data = el.getAsJsonObject();
							
							if(data.get("sys_proxy") == null || data.get("sys_proxy").isJsonNull()) {
								proxiesIndexStr = "NOPROXY";
							} else {
								proxiesIndexStr = data.get("sys_proxy").getAsString();
							}
							if(!proxies.containsKey(proxiesIndexStr)) {
								proxies.put(proxiesIndexStr, new ArrayList<ChkPayWorkerModel>());
							}
							
							proxies.get(proxiesIndexStr).add(new ChkPayWorkerModel(prodId, el, contractNoColumnName));
						}	
					}
										
					LOG.info("Start sent to thread pool prd: " + prodId);
					proxySet = proxies.entrySet();
					for (Entry<String, List<ChkPayWorkerModel>> proxyEnt : proxySet) {
						if(!chkPayPools.containsKey(proxyEnt.getKey())) {
							LOG.info("Create pool for " + proxyEnt.getKey());
							chkPayPools.put(proxyEnt.getKey(), (ThreadPoolExecutor)Executors.newFixedThreadPool(chkPoolSize));
						}
						
						LOG.info("Execute " + proxyEnt.getKey() + " size: " + proxyEnt.getValue().size());
						executor.execute(new ChkPayProxyWorker(chkPayPools.get(proxyEnt.getKey()), token, proxyEnt.getKey(), proxyEnt.getValue()));
					}
					
					proxies.clear();
					Thread.sleep(10000);
					
					while(executor.getActiveCount() != 0){
						LOG.debug("=============: CHK Manager Worker active count : " + executor.getActiveCount());
						Thread.sleep(1000);
					}
					LOG.info("Finished product " + prodId);
				}
				
				LOG.info("Finished all product");
			} catch (Exception e) {
				LOG.error(e.toString(), e);
			}
		}
	}
	
}
