package com.may.ple.kyschkpay;

import java.net.InetSocketAddress;
import java.net.Proxy;
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
	public static Map<String, Map<String, String>> firstLoginMap = new HashMap<>();
	private static final Logger LOG = Logger.getLogger(ManageLoginWorkerThread.class.getName());
	private Map<String, ThreadPoolExecutor> loginPools = new HashMap<>();
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	private static final int LOGIN_POOL_SIZE = 3;
	private static final int ITEMS_PER_PAGE = 1000;
	private List<String> proxiesIndex = new ArrayList<>();
	private List<String> prodIds;
	
	public ManageLoginWorkerThread(List<String> prodIds) {
		this.prodIds = prodIds;
	}
	
	private void initProxy() {
		String property = App.prop.getProperty("proxies");
		
		if(StringUtils.isNotBlank(property)) {
			String[] proxies = property.split(",");
			for (String proxy : proxies) {
				LOG.info("Add to proxy list : " + proxy);
				proxiesIndex.add(proxy.trim());
				loginPools.put(proxy.trim(), (ThreadPoolExecutor)Executors.newFixedThreadPool(LOGIN_POOL_SIZE));
			}
		}
	}
	
	@Override
	public void run() {
		DMSApi dmsApi = DMSApi.getInstance();
		initProxy();
		
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(loginPools.size());
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
				
				if(isClosed && KYSApi.getInstance().getESLLandPage(null) == null) {
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
				
				//-- Do first login.
				prepareFirstLogin();
				
				for (String prodId : prodIds) {
					LOG.info("Start for product id: " + prodId);
					
					currentPage = 1;
					proxyIndex = 0;
					proxySize = 0;
					proxies = new HashMap<>();
					for (String prxIndex : proxiesIndex) {
						proxies.put(prxIndex, new ArrayList<LoginWorkerModel>());
					}
					
					LOG.info("Call getChkList");
					loginChkList = dmsApi.getChkList(token, prodId, currentPage, ITEMS_PER_PAGE, "LOGIN");
					if(loginChkList == null) {
						LOG.info("Not found loginChkList");
						continue;
					}
					
					totalItems = loginChkList.get("totalItems").getAsInt();
					totalPages = (int)Math.ceil((double)totalItems / (double)ITEMS_PER_PAGE);
					numOfEachProxy = totalItems / proxiesIndex.size();
					
					LOG.info("numOfEachProxy: " + numOfEachProxy + ", totalItems: " + totalItems);
					if(totalItems == 0) {
						LOG.info("Wait 30 sec");
						Thread.sleep(30000);
						continue;
					}
					
					for (; currentPage <= totalPages; currentPage++) {
						if(currentPage > 1) {
							Thread.sleep(500);
							LOG.info("Call getChkList");
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
											loginPools.get(proxiesIndex.get(proxyIndex)),
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
							loginPools.get(proxiesIndex.get(proxyIndex)),
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
	
	private void doFirstLogin(JsonArray acc, String prxIndex, Proxy proxy, String prodId, String loanType) throws Exception {
		try {
			Map<String, String> secondLoginPage = null;
			if(acc != null) {
				// key format: productId#proxy#loanType
				String key = prodId+"#"+prxIndex+"#"+loanType;
				
				if(firstLoginMap.containsKey(key) && firstLoginMap.get(key) != null) {
					LOG.info("key: " + key + " already had.");
					return;
				}
				
				int round = 0;
				while(secondLoginPage == null) {
					LOG.info("Do first login key: " + key + " round: " + round);
					
					LOG.info("Call firstLogin");
					secondLoginPage = KYSApi.getInstance().firstLogin(proxy, acc.get(0).getAsString(), acc.get(1).getAsString());
					if(secondLoginPage == null) {
						LOG.warn("First Login fail.");
						Thread.sleep(1000);									
					} else {
						secondLoginPage.put("username", acc.get(0).getAsString());
						secondLoginPage.put("password", acc.get(1).getAsString());
					}
					round++;
				}
				
				firstLoginMap.put(key, secondLoginPage);
				LOG.info("Do first login SUCCESS key : " + key);
			}
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	public static synchronized Map<String, String> firstLoginGate(Proxy proxy, String username, String password, String oldSessionId, Map<String, String> currentSessionId) throws Exception {
		try {
			LOG.info("Check before first login with oldSessionId: " + oldSessionId);
			if(oldSessionId.equals(currentSessionId.get("sessionId"))) {
				LOG.info("## relogin");
				return KYSApi.getInstance().firstLogin(proxy, username, password);
			} else {
				LOG.info("## Curent sesson is WORK.");
				return currentSessionId;
			}
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private void prepareFirstLogin() throws Exception {
		try {
			LOG.info("Start do first login.");
			String kys = "kys", kro = "kro", kysNormal = "kys_normal";
			JsonArray kysAcc, kroAcc, kysNormalAcc;
			Proxy proxy = null;
			String[] proxyStr;
			JsonObject auth;
			
			for (String prodId : prodIds) {
				auth = App.auth.get(prodId);
				kysAcc = auth.getAsJsonArray(kys);
				kroAcc = auth.getAsJsonArray(kro);
				kysNormalAcc = auth.getAsJsonArray(kysNormal);
				
				for (String prxIndex : proxiesIndex) {
					if(!prxIndex.equals("NOPROXY")) {
						proxyStr = prxIndex.split(":");
						proxy = new Proxy(
								Proxy.Type.HTTP,
								InetSocketAddress.createUnresolved(proxyStr[0], Integer.parseInt(proxyStr[1]))
								);
					}
					
					if(kysAcc.size() > 0) {
						doFirstLogin(kysAcc, prxIndex, proxy, prodId, kys);
					}
					if(kroAcc.size() > 0) {
						doFirstLogin(kroAcc, prxIndex, proxy, prodId, kro);
					}
					if(kysNormalAcc.size() > 0) {
						doFirstLogin(kysNormalAcc, prxIndex, proxy, prodId, kysNormal);
					}
				}
			}
			LOG.info("End do first login.");
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
