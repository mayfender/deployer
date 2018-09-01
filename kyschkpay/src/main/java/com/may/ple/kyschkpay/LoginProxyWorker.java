package com.may.ple.kyschkpay;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class LoginProxyWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(LoginProxyWorker.class.getName());
	private List<UpdateChkLstModel> loginList = new ArrayList<>();
	private final int LIMITED_UPDATE_SIZE = 100;
	private List<LoginWorkerModel> worker;
	private ThreadPoolExecutor executor;
	private Proxy proxy;
	private String proxyStr;
	private String msgIndex;
	private String token;
	
	public LoginProxyWorker(ThreadPoolExecutor executor, String token, String proxyStr, List<LoginWorkerModel> worker) {
		this.executor = executor;
		this.worker = worker;
		this.token = token;
		this.proxyStr = proxyStr;
		
		if(!proxyStr.equals("NOPROXY")) {			
			String[] proxyArr = proxyStr.split(":");
			this.proxy = new Proxy(
					Proxy.Type.HTTP,
					InetSocketAddress.createUnresolved(proxyArr[0], Integer.parseInt(proxyArr[1]))
					);
		}
		this.msgIndex = (proxyStr != null ? proxyStr.toString() : "No Proxy");
	}
	
	@Override
	public void run() {
		try {
			Map<String, String> secondLogin;
			String loanType = "kys"; //TODO: loadType should be got from loginWorkerModel object.
			String key;
			
			for (LoginWorkerModel loginWorkerModel : worker) {
				key = loginWorkerModel.getProductId()+"#"+proxyStr+"#"+loanType;
				secondLogin = ManageLoginWorkerThread.firstLoginMap.get(key);
				
				if(secondLogin == null) {
					LOG.error(key + " Not found.");
					continue;
				}
				
				executor.execute(new LoginWorker(this, proxy, loginWorkerModel, secondLogin));
			}
			
			LOG.info(msgIndex + " Assign Worker finished");
			
			Thread.sleep(10000);
			while(executor.getActiveCount() != 0){
				LOG.debug(msgIndex + " =============: Proxy Worker active count : " + executor.getActiveCount());
				Thread.sleep(5000);
			}
			
			updateLoginStatus();
			LOG.info(msgIndex + " Finished");
		} catch (Exception e) {
			LOG.error(e.toString(), e);
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
			if(loginList.size() == 0) {
				LOG.warn("###### Nothing to update #######.");
				return;
			}
			
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
