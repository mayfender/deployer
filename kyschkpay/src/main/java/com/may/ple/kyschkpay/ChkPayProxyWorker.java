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

public class ChkPayProxyWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(ChkPayProxyWorker.class.getName());
	private List<UpdateChkLstModel> chkPayList = new ArrayList<>();
	private final int LIMITED_UPDATE_SIZE = 500;
	private List<ChkPayWorkerModel> worker;
	private ThreadPoolExecutor executor;
	private Proxy proxy;
	private String proxyStr;
	private String msgIndex;
	private String token;
	
	public ChkPayProxyWorker(ThreadPoolExecutor executor, String token, String proxyStr, List<ChkPayWorkerModel> worker) {
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
			
			for (ChkPayWorkerModel chkPayWorkerModel : worker) {
				key = chkPayWorkerModel.getProductId()+"#"+proxyStr+"#"+loanType;
				secondLogin = ManageLoginWorkerThread.firstLoginMap.get(key);
				
				if(secondLogin == null) {
					LOG.warn(key + " Skip to ChkPayWorker worker.");
					continue;
				}
				
				executor.execute(new ChkPayWorker(this, proxy, chkPayWorkerModel, secondLogin));
			}
			
			LOG.info(msgIndex + " Assign Worker finished");
			
			executor.shutdown();
			Thread.sleep(10000);
			while(executor.getActiveCount() != 0 || !executor.awaitTermination(1, TimeUnit.HOURS)){
				LOG.debug(msgIndex + " =============: Proxy Worker active count : " + executor.getActiveCount());
				Thread.sleep(5000);
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
				obj.addProperty("loanTypePay", modelLst.getLoanTypePay());
				
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
