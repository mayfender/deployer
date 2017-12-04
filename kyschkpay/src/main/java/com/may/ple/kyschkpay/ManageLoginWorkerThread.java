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
		
		//--: Local
		proxiesIndex.add("NOPROXY");
		
		//--: Free Proxies
		/*proxiesIndex.add("36.72.185.193:54214");
		proxiesIndex.add("123.176.103.44:80");
		proxiesIndex.add("110.77.187.189:54214");
		proxiesIndex.add("13.115.180.145:8080");
		proxiesIndex.add("13.230.154.231:8080");*/
		
		//--: Paid Proxies https://instantproxies.com
		/*proxiesIndex.add("192.126.158.195:3128");
		proxiesIndex.add("192.126.158.250:3128");
		proxiesIndex.add("173.234.249.192:3128");
		proxiesIndex.add("108.62.70.195:3128");
		proxiesIndex.add("89.32.64.226:3128");
		proxiesIndex.add("89.32.64.217:3128");				
		proxiesIndex.add("173.234.249.26:3128");
		proxiesIndex.add("173.234.249.3:3128");
		proxiesIndex.add("173.234.249.189:3128");
		proxiesIndex.add("108.62.70.15:3128");*/
		return proxiesIndex;
	}

	@Override
	public void run() {
		DMSApi dmsApi = DMSApi.getInstance();
		List<String> proxiesIndex = initProxy();
		
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(POOL_SIZE);
		Map<String, List<LoginWorkerModel>> proxies;
		String birthDateColumnName;
		String idCardNoColumnName;
		JsonElement checkList;
		JsonArray jsonArray;
		JsonObject loginChkList;
		int numOfEachProxy;
		int currentPage;
		int proxySize = 0;
		int proxyIndex;
		int totalItems;
		int totalPages;
		
		while(true) {
			try {
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
					
					currentPage = 1;
					proxyIndex = 0;
					proxies = new HashMap<>();
					for (String prxIndex : proxiesIndex) {
						proxies.put(prxIndex, new ArrayList<LoginWorkerModel>());
					}
					
					loginChkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "LOGIN");
					if(loginChkList == null) break;
					
					totalItems = loginChkList.get("totalItems").getAsInt();
					totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					numOfEachProxy = totalItems / proxiesIndex.size();
					
					LOG.debug("numOfEachProxy: " + numOfEachProxy);
					LOG.debug("totalItems: " + totalItems);
					if(totalItems == 0) continue;
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {							
							loginChkList = dmsApi.getChkList(prodId, currentPage, ITEMS_PER_PAGE, "LOGIN");
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
									LOG.debug("proxyIndex: " + proxyIndex);
									LOG.debug("proxySize: " + proxySize);
									
									executor.execute(new LoginProxyWorker(
											proxiesIndex.get(proxyIndex), 
											proxies.get(proxiesIndex.get(proxyIndex))
											));
									
									proxyIndex++;
									proxySize = 0;
								}
							}
						}
						
						LOG.debug("Execute Last Worker Group");
						executor.execute(new LoginProxyWorker(
								proxiesIndex.get(proxyIndex), 
								proxies.get(proxiesIndex.get(proxyIndex))
								));
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
					Thread.sleep(60000);
				} catch (Exception e2) {}
			}
		}
	}
	
}
