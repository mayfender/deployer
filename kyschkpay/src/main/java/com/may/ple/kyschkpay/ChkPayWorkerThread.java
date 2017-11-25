package com.may.ple.kyschkpay;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ChkPayWorkerThread implements Runnable {
	private static final Logger LOG = Logger.getLogger(ChkPayWorkerThread.class.getName());
	private JsonElement element;
	private String id;
	private String productId;
	private String sessionId;
	private String cif;
	private String url;
	private String loanType;
	private String accNo;
	private Double totalPayInstallmentOld;
	private Double preBalanceOld;
	private Double lastPayAmountOld;
	private String contractNoColumnName;
	private String contractNo;
	
	public ChkPayWorkerThread(String productId, JsonElement element, String contractNoColumnName) {
		this.element = element;
		this.productId = productId;
		this.contractNoColumnName = contractNoColumnName;
	}

	@Override
	public void run() {
		try {
			JsonObject data = element.getAsJsonObject();
			this.id = data.get("_id").getAsString();
			this.sessionId = data.get("sys_sessionId").getAsString();
			this.cif = data.get("sys_cif").getAsString();
			this.url = data.get("sys_uri").getAsString();
			this.loanType = data.get("sys_loanType").getAsString();
			this.accNo = data.get("sys_accNo").getAsString();
			this.contractNo = data.get(contractNoColumnName).getAsString();
			
			JsonElement totalPayInstallment, preBalance, lastPayAmount;
			if((totalPayInstallment = data.get("sys_totalPayInstallment")) != null) {
				this.totalPayInstallmentOld = totalPayInstallment.getAsDouble();				
			} else {
				this.totalPayInstallmentOld = -1d;
			}
			if((preBalance = data.get("sys_preBalance")) != null) {
				this.preBalanceOld = preBalance.getAsDouble();				
			} else {
				this.preBalanceOld = -1d;				
			}
			if((lastPayAmount = data.get("sys_lastPayAmount")) != null) {
				this.lastPayAmountOld = lastPayAmount.getAsDouble();				
			} else {
				this.lastPayAmountOld = -1d;				
			}
			
			chkPay();
			
			LOG.info("Worker end : ");
		} catch (Exception e) {
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setId(this.id);
			model.setErrMsg(e.toString());
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			model.setCreatedDateTime(new Date());
			
			App.chkPayWorker.addToChkPayList(model, productId);
			LOG.error(e.toString());
		}
	}
	
	private void chkPay() throws Exception {
		UpdateChkLstModel model = new UpdateChkLstModel();
		model.setStatus(StatusConstant.UPDATE_CHKPAY.getStatus());
		model.setId(this.id);
		
		try {
			PaymentModel paymentInfo;
			int count = 0;
			
			while(true) {
				if(count == 10) {
					LOG.warn("Cann't get paymentInfo");
					return;
				}
				
				paymentInfo = KYSApi.getInstance().getPaymentInfo(this.sessionId, this.cif, this.url, this.loanType, this.accNo);
				if(!paymentInfo.isError()) break;
				
				LOG.warn("Round[" + count + "] :=================: KYS Error :=============: sessionId " + this.sessionId);
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
				
				if(DateUtils.isSameDay(lastPayDate, today)) {
					if(lastPayAmountOld != lastPayAmount ||
						totalPayInstallmentOld.doubleValue() != totalPayInstallment ||
						preBalanceOld.doubleValue() != preBalance) {
						
						LOG.info("==================: Have Paid :===================");
						model.setStatus(StatusConstant.UPDATE_CHKPAY_PAID.getStatus());
						model.setLastPayDate(lastPayDate);
						model.setLastPayAmount(lastPayAmount);
						model.setTotalPayInstallment(totalPayInstallment);
						model.setPreBalance(preBalance);
						model.setContractNo(this.contractNo);
						model.setHtml(paymentInfo.getHtml());
					}
				}		
			}
		} catch(CustomException e) {
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			model.setErrMsg("Check Pay Session Timeout");
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
		
		model.setCreatedDateTime(new Date());
		App.chkPayWorker.addToChkPayList(model, this.productId);
	}
	
}
