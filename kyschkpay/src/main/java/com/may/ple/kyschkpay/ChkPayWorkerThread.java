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
	
	public ChkPayWorkerThread(String productId, JsonElement element) {
		this.element = element;
		this.productId = productId;
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
			
			JsonElement totalPayInstallment;
			JsonElement preBalance;
			if((totalPayInstallment = data.get("sys_totalPayInstallment")) != null) {
				this.totalPayInstallmentOld = totalPayInstallment.getAsDouble();				
			}
			if((preBalance = data.get("sys_preBalance")) != null) {
				this.preBalanceOld = preBalance.getAsDouble();				
			}
			
			chkPay();
			
			LOG.info("Worker end : ");
		} catch (Exception e) {
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setId(this.id);
			model.setErrMsg(e.toString());
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			
			App.chkPayWorker.addToLoginList(model, productId);
			LOG.error(e.toString());
		}
	}
	
	private void chkPay() throws Exception {
		UpdateChkLstModel model = new UpdateChkLstModel();
		model.setStatus(StatusConstant.UPDATE_CHKPAY.getStatus());
		model.setId(this.id);
		
		try {
			PaymentModel paymentInfo = KYSApi.getInstance().getPaymentInfo(this.sessionId, this.cif, this.url, this.loanType, this.accNo);
			Date lastPayDate = paymentInfo.getLastPayDate();
			double totalPayInstallment = paymentInfo.getTotalPayInstallment().doubleValue();
			double preBalance = paymentInfo.getPreBalance().doubleValue();
			Date today = Calendar.getInstance().getTime();
			
			if(DateUtils.isSameDay(lastPayDate, today)) {
				if(totalPayInstallmentOld == null || 
						preBalanceOld == null || 
						totalPayInstallmentOld.doubleValue() != totalPayInstallment ||
						preBalanceOld.doubleValue() != preBalance) {
					
					model.setStatus(StatusConstant.UPDATE_CHKPAY_PAID.getStatus());
					model.setLastPayDate(lastPayDate);
					model.setLastPayAmount(paymentInfo.getLastPayAmount());
					model.setTotalPayInstallment(totalPayInstallment);
					model.setPreBalance(preBalance);
				}
			}		
		} catch(CustomException e) {
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			model.setErrMsg("Check Pay Session Timeout");
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
		
		App.chkPayWorker.addToLoginList(model, this.productId);
	}
	
}
