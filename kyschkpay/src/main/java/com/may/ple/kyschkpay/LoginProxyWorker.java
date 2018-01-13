package com.may.ple.kyschkpay;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class LoginProxyWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(LoginProxyWorker.class.getName());
	private List<UpdateChkLstModel> loginList = new ArrayList<>();
	private final int LIMITED_UPDATE_SIZE = 100;
	private static final int POOL_SIZE = 3;
	private List<LoginWorkerModel> worker;
	private Proxy proxy;
	private String msgIndex;
	private String token;
	
	public LoginProxyWorker(String token, String proxy, List<LoginWorkerModel> worker) {
		this.worker = worker;
		this.token = token;
		
		if(!proxy.equals("NOPROXY")) {			
			String[] proxyStr = proxy.split(":");
			this.proxy = new Proxy(
					Proxy.Type.HTTP,
					InetSocketAddress.createUnresolved(proxyStr[0], Integer.parseInt(proxyStr[1]))
					);
		}
		this.msgIndex = (proxy != null ? proxy.toString() : "No Proxy");
	}
	
	@Override
	public void run() {
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(POOL_SIZE);
		
		try {
			for (LoginWorkerModel loginWorkerModel : worker) {
				executor.execute(new LoginWorker(this, proxy, loginWorkerModel));
			}
			
			LOG.info(msgIndex + " Assign Worker finished");
			
			Thread.sleep(10000);
			while(executor.getActiveCount() != 0){
				LOG.debug(msgIndex + " =============: Proxy Worker active count : " + executor.getActiveCount());
				Thread.sleep(1000);
			}
			
			updateLoginStatus();
			LOG.info(msgIndex + " Finished");
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		} finally {
			executor.shutdownNow();
		}
	}
	
	public synchronized void addToLoginList(UpdateChkLstModel model) {
		try {
			loginList.add(model);
			LOG.debug(msgIndex + " addToLoginList");
			
			if(loginList.size() == LIMITED_UPDATE_SIZE) {
				updateLoginStatus();
				loginList.clear();
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	private void updateLoginStatus() throws Exception {
		try {
			if(loginList.size() == 0) return;
			
			LOG.info(msgIndex + " Update login success size: " + loginList.size());
			JsonArray array = new JsonArray();
			JsonObject obj;
			
			for (UpdateChkLstModel modelLst : loginList) {
				obj = new JsonObject();
				obj.addProperty("id", modelLst.getId());
				obj.addProperty("productId", modelLst.getProductId());
				obj.addProperty("accNo", modelLst.getAccNo());
				obj.addProperty("cif", modelLst.getCif());
				obj.addProperty("errMsg", modelLst.getErrMsg());
				obj.addProperty("flag", modelLst.getFlag());
				obj.addProperty("loanType", modelLst.getLoanType());
				obj.addProperty("sessionId", modelLst.getSessionId());
				obj.addProperty("status", modelLst.getStatus());
				obj.addProperty("uri", modelLst.getUri());
				obj.addProperty("createdDateTime", modelLst.getCreatedDateTime().getTime());
				obj.addProperty("proxy", modelLst.getProxy());
				array.add(obj);
			}
			
			DMSApi.getInstance().updateStatus(this.token, array);
			LOG.info(msgIndex + "End updateLoginStatus");
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
