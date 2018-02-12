package com.may.ple.kyschkpay;

import java.net.Proxy;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ChkPayWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(ChkPayWorker.class.getName());
	private ChkPayProxyWorker proxyWorker;
	private ChkPayWorkerModel chkPayModel;
	private Proxy proxy;
	private String id;
	private String sessionId;
	private String cif;
	private String url;
	private String loanType;
	private String accNo;
	private Double totalPayInstallmentOld;
	private Double preBalanceOld;
	private Date lastPayDateOld;
	private Double lastPayAmountOld;
	private String contractNo;
	private String msgIndex;
	
	public ChkPayWorker(ChkPayProxyWorker proxyWorker, Proxy proxy, ChkPayWorkerModel chkPayWorkerModel) {
		this.proxyWorker = proxyWorker;
		this.proxy = proxy;
		this.chkPayModel = chkPayWorkerModel;
		this.msgIndex = (proxy != null ? proxy.toString() : "No Proxy");
	}
	
	@Override
	public void run() {
		try {
			JsonObject data = this.chkPayModel.getJsonElement().getAsJsonObject();
			this.id = data.get("_id").getAsString();
			this.sessionId = data.get("sys_sessionId").getAsString();
			this.cif = data.get("sys_cif").getAsString();
			this.url = data.get("sys_uri").getAsString();
			this.loanType = data.get("sys_loanType").getAsString();
			this.accNo = data.get("sys_accNo").getAsString();
			this.contractNo = data.get(this.chkPayModel.getContractNoColumnName()).getAsString();
			
			JsonElement element;
			if((element = data.get("sys_totalPayInstallment")) != null) {
				this.totalPayInstallmentOld = element.getAsDouble();				
			} else {
				this.totalPayInstallmentOld = -1d;
			}
			if((element = data.get("sys_preBalance")) != null) {
				this.preBalanceOld = element.getAsDouble();				
			} else {
				this.preBalanceOld = -1d;				
			}
			if((element = data.get("sys_lastPayDate")) != null) {
				this.lastPayDateOld = new Date(element.getAsLong());
			}
			if((element = data.get("sys_lastPayAmount")) != null) {
				this.lastPayAmountOld = element.getAsDouble();				
			} else {
				this.lastPayAmountOld = -1d;				
			}
			
			chkPay();
			
			LOG.debug("Worker end : ");
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	private void chkPay() throws Exception {
		UpdateChkLstModel model = new UpdateChkLstModel();
		model.setStatus(StatusConstant.UPDATE_CHKPAY.getStatus());
		model.setId(this.id);
		model.setProductId(this.chkPayModel.getProductId());
		
		try {
			PaymentModel paymentInfo;
			int count = 0;
			
			while(true) {
				if(count == 5) {
					LOG.error("Cann't get paymentInfo");
					throw new CustomException(1, "Cann't get paymentInfo");
				}
				
				paymentInfo = KYSApi.getInstance().getPaymentInfo(this.proxy, this.sessionId, this.cif, this.url, this.loanType, this.accNo);
				if(!paymentInfo.isError()) break;
				
				LOG.warn(msgIndex + " Round[" + count + "] :=================: KYS Error :=============: sessionId " + this.sessionId);
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
				boolean isPaid = Boolean.FALSE;
				
				/*Calendar calendar = Calendar.getInstance();
				calendar.set(2017, 6, 14);
				Date today = calendar.getTime();*/
				
				if(DateUtils.isSameDay(lastPayDate, today)) {
					if(this.lastPayAmountOld != lastPayAmount ||
							this.totalPayInstallmentOld.doubleValue() != totalPayInstallment ||
									this.preBalanceOld.doubleValue() != preBalance) {
						
						LOG.info("==================: Have Paid with option 1 :===================");
						isPaid = true;
					}
				} else if(this.lastPayDateOld == null) {
					LOG.info("==================: Have Paid with option 2 :===================");
					isPaid = true;					
				} else if(this.lastPayDateOld != null) {
					LocalDate lastPayDateOldLocalDate = this.lastPayDateOld.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					LocalDate lastPayDateLocalDate = lastPayDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					
					if(lastPayDateOldLocalDate.isBefore(lastPayDateLocalDate)) {
						LOG.info("==================: Have Paid with option 3 :===================");
						isPaid = true;
					}
				}
				
				if(isPaid) {
					model.setStatus(StatusConstant.UPDATE_CHKPAY_PAID.getStatus());
					model.setLastPayDate(lastPayDate);
					model.setLastPayAmount(lastPayAmount);
					model.setTotalPayInstallment(totalPayInstallment);
					model.setPreBalance(preBalance);
					model.setContractNo(this.contractNo);
					model.setHtml(paymentInfo.getDoc().html());
				}
			}
		} catch(CustomException e) {
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			model.setErrMsg("Check Pay Session Timeout");
			LOG.error(msgIndex + " [sessionId " + sessionId + "] ############## " + e.toString());
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
		
		model.setCreatedDateTime(new Date());
		proxyWorker.addToChkPayList(model);
		LOG.info(msgIndex + " End checkpay");
	}
	
}
