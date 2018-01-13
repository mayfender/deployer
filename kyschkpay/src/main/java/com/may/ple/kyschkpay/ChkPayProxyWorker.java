package com.may.ple.kyschkpay;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ChkPayProxyWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(ChkPayProxyWorker.class.getName());
	private List<UpdateChkLstModel> chkPayList = new ArrayList<>();
	private final int LIMITED_UPDATE_SIZE = 500;
	private List<ChkPayWorkerModel> worker;
	private ThreadPoolExecutor executor;
	private Proxy proxy;
	private String msgIndex;
	private String token;
	
	public ChkPayProxyWorker(ThreadPoolExecutor executor, String token, String proxy, List<ChkPayWorkerModel> worker) {
		this.executor = executor;
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
		try {
			for (ChkPayWorkerModel chkPayWorkerModel : worker) {
				executor.execute(new ChkPayWorker(this, proxy, chkPayWorkerModel));
			}
			
			LOG.info(msgIndex + " Assign Worker finished");
			
			Thread.sleep(10000);
			while(executor.getActiveCount() != 0){
				LOG.debug(msgIndex + " =============: Proxy Worker active count : " + executor.getActiveCount());
				Thread.sleep(1000);
			}
			
			LOG.info(msgIndex + " all size: " + chkPayList.size());
			updateChkPayStatus();
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	public synchronized void addToChkPayList(UpdateChkLstModel model) {
		try {
			chkPayList.add(model);
			LOG.debug(msgIndex + " chkPayList size: " + chkPayList.size());
			
			if(chkPayList.size() == LIMITED_UPDATE_SIZE) {
				LOG.info("Call updateChkPayStatus");
				updateChkPayStatus();
				chkPayList.clear();
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	private synchronized void updateChkPayStatus() throws Exception {
		try {
			if(chkPayList.size() == 0) return;
			
			LOG.info(msgIndex + " Update check payment");
			JsonArray array = new JsonArray();
			JsonObject obj;
			
			for (UpdateChkLstModel modelLst : chkPayList) {
				obj = new JsonObject();
				obj.addProperty("id", modelLst.getId());
				obj.addProperty("productId", modelLst.getProductId());
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
			
			LOG.info(msgIndex + " Call updateLoginStatus size: " + array.size());
			DMSApi.getInstance().updateStatus(this.token, array);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
