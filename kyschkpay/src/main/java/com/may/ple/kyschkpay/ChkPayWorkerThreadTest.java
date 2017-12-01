package com.may.ple.kyschkpay;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ChkPayWorkerThreadTest implements Runnable {
	private static final Logger LOG = Logger.getLogger(ChkPayWorkerThreadTest.class.getName());
	private final int LIMITED_UPDATE_SIZE = 1000;
	private List<UpdateChkLstModel> chkPayList = new ArrayList<>();
	private List<ChkPayWorkerModel> worker;
	private Proxy proxy;
	
	public ChkPayWorkerThreadTest(String proxy, List<ChkPayWorkerModel> worker) {
		this.worker = worker;
		
		if(!proxy.equals("NOPROXY")) {			
			String[] proxyStr = proxy.split(":");
			this.proxy = new Proxy(
					Proxy.Type.HTTP,                                      
					InetSocketAddress.createUnresolved(proxyStr[0], Integer.parseInt(proxyStr[1]))
					);
		}
	}
	
	@Override
	public void run() {
		try {
			String id, sessionId, cif, url, loanType, accNo, contractNo, productId;
			Double totalPayInstallmentOld;
			Double lastPayAmountOld;
			Double preBalanceOld;
			
			for (ChkPayWorkerModel chkPayWorkerModel : worker) {
				JsonObject data = chkPayWorkerModel.getJsonElement().getAsJsonObject();
				id = data.get("_id").getAsString();
				sessionId = data.get("sys_sessionId").getAsString();
				cif = data.get("sys_cif").getAsString();
				url = data.get("sys_uri").getAsString();
				loanType = data.get("sys_loanType").getAsString();
				accNo = data.get("sys_accNo").getAsString();
				contractNo = data.get(chkPayWorkerModel.getContractNoColumnName()).getAsString();
				productId = chkPayWorkerModel.getProductId();
				
				JsonElement totalPayInstallment, preBalance, lastPayAmount;
				if((totalPayInstallment = data.get("sys_totalPayInstallment")) != null) {
					totalPayInstallmentOld = totalPayInstallment.getAsDouble();				
				} else {
					totalPayInstallmentOld = -1d;
				}
				if((preBalance = data.get("sys_preBalance")) != null) {
					preBalanceOld = preBalance.getAsDouble();				
				} else {
					preBalanceOld = -1d;				
				}
				if((lastPayAmount = data.get("sys_lastPayAmount")) != null) {
					lastPayAmountOld = lastPayAmount.getAsDouble();				
				} else {
					lastPayAmountOld = -1d;				
				}
				
				ChkModel chkModel = new ChkModel();
				chkModel.id = id;
				chkModel.productId = productId;
				chkModel.sessionId = sessionId;
				chkModel.cif = cif;
				chkModel.url = url;
				chkModel.loanType = loanType;
				chkModel.accNo = accNo;
				chkModel.contractNo = contractNo;
				chkModel.totalPayInstallmentOld = totalPayInstallmentOld;
				chkModel.lastPayAmountOld = lastPayAmountOld;
				chkModel.preBalanceOld = preBalanceOld;
				
				chkPay(chkModel);
				
				LOG.info("Worker end : ");
			}
			
			LOG.debug((proxy != null ? proxy.toString() : "No Proxy") + "chkPayList size: " + chkPayList.size());
			updateChkPayStatus();
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	private void chkPay(ChkModel chkModel) throws Exception {
		UpdateChkLstModel model = new UpdateChkLstModel();
		model.setStatus(StatusConstant.UPDATE_CHKPAY.getStatus());
		model.setId(chkModel.id);
		model.setProductId(chkModel.productId);
		
		try {
			PaymentModel paymentInfo;
			int count = 0;
			
			while(true) {
				if(count == 10) {
					LOG.warn("Cann't get paymentInfo");
					return;
				}
				
				paymentInfo = KYSApi.getInstance().getPaymentInfo(this.proxy, chkModel.sessionId, chkModel.cif, chkModel.url, chkModel.loanType, chkModel.accNo);
				if(!paymentInfo.isError()) break;
				
				LOG.warn("Round[" + count + "] :=================: KYS Error :=============: sessionId " + chkModel.sessionId);
				count++;
				Thread.sleep(5000);
			}
			
			if(paymentInfo.isRefresh()) {
				LOG.debug("Refresh Mode");
				return;
			}
			
			Date lastPayDate = paymentInfo.getLastPayDate();
			if(lastPayDate != null) {
				double totalPayInstallment = paymentInfo.getTotalPayInstallment().doubleValue();
				double preBalance = paymentInfo.getPreBalance().doubleValue();
				double lastPayAmount = paymentInfo.getLastPayAmount().doubleValue();
				Date today = Calendar.getInstance().getTime();
				
				/*Calendar calendar = Calendar.getInstance();
				calendar.set(2017, 6, 14);
				Date today = calendar.getTime();*/
				
				if(DateUtils.isSameDay(lastPayDate, today)) {
					if(chkModel.lastPayAmountOld != lastPayAmount ||
							chkModel.totalPayInstallmentOld.doubleValue() != totalPayInstallment ||
									chkModel.preBalanceOld.doubleValue() != preBalance) {
						
						LOG.info("==================: Have Paid :===================");
						model.setStatus(StatusConstant.UPDATE_CHKPAY_PAID.getStatus());
						model.setLastPayDate(lastPayDate);
						model.setLastPayAmount(lastPayAmount);
						model.setTotalPayInstallment(totalPayInstallment);
						model.setPreBalance(preBalance);
						model.setContractNo(chkModel.contractNo);
						model.setHtml(paymentInfo.getHtml());
					}
				}		
			}
		} catch(CustomException e) {
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			model.setErrMsg("Check Pay Session Timeout");
		} catch (IOException e) {
			LOG.error(e.toString());
			Thread.sleep(60000);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
		
		model.setCreatedDateTime(new Date());
		addToChkPayList(model);
	}
	
	public void addToChkPayList(UpdateChkLstModel model) {
		try {
			chkPayList.add(model);
			LOG.debug("chkPayList size: " + chkPayList.size());
			
			if(chkPayList.size() == LIMITED_UPDATE_SIZE) {
				LOG.info("Call updateChkPayStatus");
				updateChkPayStatus();
				chkPayList.clear();
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	private void updateChkPayStatus() throws Exception {
		try {
			if(chkPayList.size() == 0) return;
			
			LOG.info("Update check payment");
			JsonArray array = new JsonArray();
			JsonObject obj;
			
			for (UpdateChkLstModel modelLst : chkPayList) {
				obj = new JsonObject();
				obj.addProperty("id", modelLst.getId());
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
			
			LOG.info("Call updateLoginStatus");
			DMSApi.getInstance().updateStatus(array);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}

class ChkModel {
	public String id;
	public String sessionId; 
	public String cif;
	public String url; 
	public String loanType; 
	public String accNo;
	public String productId;
	public String contractNo;
	public Double totalPayInstallmentOld;
	public Double lastPayAmountOld;
	public Double preBalanceOld;
}
