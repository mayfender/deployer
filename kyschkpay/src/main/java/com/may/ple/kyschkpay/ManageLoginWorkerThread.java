package com.may.ple.kyschkpay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ManageLoginWorkerThread extends Thread {
	private static final Logger LOG = Logger.getLogger(ManageLoginWorkerThread.class.getName());
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	private static final int POOL_SIZE = 100;
	private static final int ITEMS_PER_PAGE = 1000;
	private List<String> prodIds;
	
	public ManageLoginWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}
	
	private List<String> initProxy() {
		List<String> proxiesIndex = new ArrayList<>();
		proxiesIndex.add("NOPROXY"); //--: Local
		String property = App.prop.getProperty("proxies");
		
		if(StringUtils.isNotBlank(property)) {
			String[] proxies = property.split(",");
			for (String proxy : proxies) {
				LOG.info("Add to proxy list : " + proxy);
				proxiesIndex.add(proxy.trim());
			}
		}
		return proxiesIndex;
	}

	@Override
	public void run() {
		DMSApi dmsApi = DMSApi.getInstance();
		List<String> proxiesIndex = initProxy();
		
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(POOL_SIZE);
		Map<String, List<LoginWorkerModel>> proxies;
		boolean isClosed = Boolean.TRUE;
		String birthDateColumnName;
		String idCardNoColumnName;
		JsonElement checkList;
		JsonArray jsonArray;
		JsonObject loginChkList;
		String token = null;
		int numOfEachProxy;
		int currentPage;
		int proxySize;
		int proxyIndex;
		int totalItems;
		int totalPages;
		
		while(true) {
			try {
				if(!App.checkWorkingHour()) {
					LOG.debug("Sleep 1 min");
					isClosed = Boolean.TRUE;
					Thread.sleep(60000);
					continue;
				}
				
				if(isClosed && KYSApi.getInstance().getLoginPage(null) == null) {
					LOG.info("Not open now and sleep 10 min");
					Thread.sleep(600000);
					continue;
				}
				
				isClosed = Boolean.FALSE;
				
				if(StringUtils.isBlank(token = dmsApi.login(USERNAME, PASSWORD))) {
					LOG.warn("May be server is down.");
					Thread.sleep(30000);
					continue;
				}
				
				for (String prodId : prodIds) {
					LOG.info("Start for product id: " + prodId);
					
					currentPage = 1;
					proxyIndex = 0;
					proxySize = 0;
					proxies = new HashMap<>();
					for (String prxIndex : proxiesIndex) {
						proxies.put(prxIndex, new ArrayList<LoginWorkerModel>());
					}
					
					loginChkList = dmsApi.getChkList(token, prodId, currentPage, ITEMS_PER_PAGE, "LOGIN");
					if(loginChkList == null) {
						LOG.info("Not found loginChkList");
						continue;
					}
					
					totalItems = loginChkList.get("totalItems").getAsInt();
					totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					numOfEachProxy = totalItems / proxiesIndex.size();
					
					LOG.info("numOfEachProxy: " + numOfEachProxy + ", totalItems: " + totalItems);
					if(totalItems == 0) continue;
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {							
							loginChkList = dmsApi.getChkList(token, prodId, currentPage, ITEMS_PER_PAGE, "LOGIN");
							if(loginChkList == null) break;
						}
						
						idCardNoColumnName = loginChkList.get("idCardNoColumnName").getAsString();
						birthDateColumnName = loginChkList.get("birthDateColumnName").getAsString();
						checkList = loginChkList.get("checkList");
						
						if(checkList == null) continue;
						
						jsonArray = checkList.getAsJsonArray();
						
						for (JsonElement el : jsonArray) {
							proxies.get(proxiesIndex.get(proxyIndex)).add(new LoginWorkerModel(prodId, el, idCardNoColumnName, birthDateColumnName));
							proxySize++;
							
							if((proxyIndex + 1) < proxiesIndex.size()) {
								if(proxySize == numOfEachProxy) {
									LOG.info("Sent to thread Pool proxyIndex: " + proxyIndex + " proxySize: " + proxySize);
									
									executor.execute(new LoginProxyWorker(
											token,
											proxiesIndex.get(proxyIndex), 
											proxies.get(proxiesIndex.get(proxyIndex))
											));
									
									proxies.put(proxiesIndex.get(proxyIndex), null);
									proxyIndex++;
									proxySize = 0;
								}
							}
						}	
					}
					
					LOG.info("Execute Last Worker Group");
					executor.execute(new LoginProxyWorker(
							token,
							proxiesIndex.get(proxyIndex), 
							proxies.get(proxiesIndex.get(proxyIndex))
					));
					
					LOG.info("Finished for product id: " + prodId);
					Thread.sleep(10000);
					while(executor.getActiveCount() != 0){
						LOG.debug("=============: LOGIN Manager Worker active count : " + executor.getActiveCount());
						Thread.sleep(1000);
					}
				}
				
				LOG.info("Finished all product");
			} catch (Exception e) {
				LOG.error(e.toString(), e);
			} finally {
				try {
					//--: Sleep 1 minutes
					Thread.sleep(60000);
				} catch (Exception e2) {}
			}
		}
	}
	
}
