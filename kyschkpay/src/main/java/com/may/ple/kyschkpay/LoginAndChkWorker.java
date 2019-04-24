package com.may.ple.kyschkpay;

import java.net.Proxy;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LoginAndChkWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(LoginAndChkWorker.class.getName());
	private Map<String, String> secondLoginPage;
	private LoginAndChkProxyWorker proxyWorker;
	private LoginAndChkWorkerModel loginModel;
	private Proxy proxy;
	private String id;
	private String sessionId;
	private String cif;
	private String idCard;
	private String birthDate;
	private String msgIndex;
	private Double totalPayInstallmentOld;
	private Double preBalanceOld;
	private Date lastPayDateOld;
	private Double lastPayAmountOld;
	private Double totalPayInstallmentKroOld;
	private Double preBalanceKroOld;
	private Date lastPayDateKroOld;
	private Double lastPayAmountKroOld;
	private String contractNo;
	
	public LoginAndChkWorker(LoginAndChkProxyWorker proxyWorker, Proxy proxy, LoginAndChkWorkerModel loginModel, Map<String, String> secondLoginPage) {
		this.proxyWorker = proxyWorker;
		this.proxy = proxy;
		this.loginModel = loginModel;
		this.secondLoginPage = secondLoginPage;
		this.msgIndex = (proxy != null ? proxy.toString() : "No Proxy");
	}
	
	@Override
	public void run() {
		try {
			JsonObject data = loginModel.getJsonElement().getAsJsonObject();
			this.id = data.get("_id").getAsString();
			this.idCard = data.get(loginModel.getIdCardNoColumnName()).getAsString();
			this.contractNo = data.get(loginModel.getContractNoColumnName()).getAsString();
			
			JsonElement jsonElement = data.get(loginModel.getBirthDateColumnName());
			if(jsonElement.isJsonNull()) {
				LOG.warn("Skip in case of birthdate is empty.");
				return;
			}
			
			String birthDateDummy = jsonElement.getAsString();
			if(birthDateDummy == null || (birthDateDummy = birthDateDummy.trim()).length() != 8) {
				LOG.warn("Skip in case of birthdate wrong format : " + birthDateDummy);
				return;
			}
			
			LOG.info("birthDate : " + birthDateDummy);
			this.birthDate = DateUtil.birthDateFormat(birthDateDummy);
			
			LOG.debug("Call second login.");
			LoginRespModel resp = KYSApi.getInstance().secondLogin(proxy, idCard, birthDate, secondLoginPage);
			
			this.sessionId = secondLoginPage.get("sessionId");
			this.cif = resp.getCif();
			
			if(StatusConstant.LOGIN_SUCCESS == resp.getStatus()) {
				List<List<String>> argsList = KYSApi.getInstance().getParam(this.proxy, this.sessionId, this.cif);
				List<CheckRespModel> checkRespModelLst = new ArrayList<>();
				CheckRespModel chkResp;
				
				for (List<String> params : argsList) {
					chkResp = new CheckRespModel();
					chkResp.setLoanType(params.get(0));
					chkResp.setFlag(params.get(5));
					chkResp.setAccNo(params.get(2));
					
					if(chkResp.getFlag().equals("1")) {
						chkResp.setUri(KYSApi.LINK + "/STUDENT/ESLMTI001.do");
					} else {
						chkResp.setUri(KYSApi.LINK + "/STUDENT/ESLMTI003.do");
					}
					checkRespModelLst.add(chkResp);
				}
				
				LOG.info("Start to get payment info.");
				chkPay(data, checkRespModelLst);
			} else {
				UpdateChkLstModel chkResp = new UpdateChkLstModel();
				chkResp.setCreatedDateTime(new Date());
				chkResp.setId(this.id);
				chkResp.setProductId(this.loginModel.getProductId());
				chkResp.setStatus(resp.getStatus().getStatus());
				addToUpdateList(chkResp);
			}
			
			LOG.debug(msgIndex + " Worker end : " + this.idCard);
		} catch (Exception e) {
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setId(this.id);
			model.setProductId(this.loginModel.getProductId());
			model.setErrMsg(e.toString());
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			model.setCreatedDateTime(new Date());
			
			proxyWorker.addToLoginList(model);
			LOG.error(e.toString());
		}
	}
	
	private void addToUpdateList(UpdateChkLstModel model) throws Exception {
		try {
			LOG.debug(msgIndex + " Start proceed " + this.sessionId);
			
			if(StatusConstant.SERVICE_UNAVAILABLE.getStatus().intValue() == model.getStatus().intValue()) {
				LOG.debug("==============: SERVICE_UNAVAILABLE :==================");
				return;
			}
			
			if(StatusConstant.LOGIN_FAIL.getStatus().intValue() == model.getStatus().intValue()) {
				//--
			} else if (StatusConstant.UPDATE_CHKPAY.getStatus().intValue() == model.getStatus().intValue()) {				
				//--
			} else if (StatusConstant.UPDATE_CHKPAY_PAID.getStatus().intValue() == model.getStatus().intValue()) {
				//--
			} else {
				LOG.error("Out of IF CASE!!!");
			}
			
			proxyWorker.addToLoginList(model);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private void chkPay(JsonObject data, List<CheckRespModel> checkRespModelLst) throws Exception {
		try {
			UpdateChkLstModel modelF101 = null, modelF201 = null;
			for (CheckRespModel checkRespModel : checkRespModelLst) {				
				if(checkRespModel.getLoanType().equals("F101")) { // กยศ
					modelF101 = new UpdateChkLstModel();
					modelF101.setId(this.id);
					modelF101.setProductId(this.loginModel.getProductId());
					modelF101.setSessionId(this.sessionId);
					modelF101.setCif(this.cif);
					modelF101.setProxy(this.proxy != null ? this.proxy.address().toString() : null);
					modelF101.setLoanType(checkRespModel.getLoanType());
					modelF101.setFlag(checkRespModel.getFlag());
					modelF101.setAccNo(checkRespModel.getAccNo());
					modelF101.setUri(checkRespModel.getUri());
				} else if(checkRespModel.getLoanType().equals("F201")) { // กรอ
					modelF201 = new UpdateChkLstModel();
					modelF201.setId(this.id);
					modelF201.setProductId(this.loginModel.getProductId());
					modelF201.setSessionId(this.sessionId);
					modelF201.setCif(this.cif);
					modelF201.setProxy(this.proxy != null ? this.proxy.address().toString() : null);
					modelF201.setLoanType(checkRespModel.getLoanType());
					modelF201.setFlag(checkRespModel.getFlag());
					modelF201.setAccNo(checkRespModel.getAccNo());
					modelF201.setUri(checkRespModel.getUri());
				}
			}
			
			JsonElement element;
			if((element = data.get("sys_totalPayInstallment")) != null && !element.isJsonNull()) {
				this.totalPayInstallmentOld = element.getAsDouble();				
			} else {
				this.totalPayInstallmentOld = -1d;
			}
			if((element = data.get("sys_preBalance")) != null && !element.isJsonNull()) {
				this.preBalanceOld = element.getAsDouble();				
			} else {
				this.preBalanceOld = -1d;				
			}
			if((element = data.get("sys_lastPayDate")) != null && !element.isJsonNull()) {
				this.lastPayDateOld = new Date(element.getAsLong());
			}
			if((element = data.get("sys_lastPayAmount")) != null && !element.isJsonNull()) {
				this.lastPayAmountOld = element.getAsDouble();				
			} else {
				this.lastPayAmountOld = -1d;				
			}
			
			//----------------------------: KRO :-----------------------------------
			if((element = data.get("sys_totalPayInstallment_kro")) != null && !element.isJsonNull()) {
				this.totalPayInstallmentKroOld = element.getAsDouble();				
			} else {
				this.totalPayInstallmentKroOld = -1d;
			}
			if((element = data.get("sys_preBalance_kro")) != null && !element.isJsonNull()) {
				this.preBalanceKroOld = element.getAsDouble();				
			} else {
				this.preBalanceKroOld = -1d;				
			}
			if((element = data.get("sys_lastPayDate_kro")) != null && !element.isJsonNull()) {
				this.lastPayDateKroOld = new Date(element.getAsLong());
			}
			if((element = data.get("sys_lastPayAmount_kro")) != null && !element.isJsonNull()) {
				this.lastPayAmountKroOld = element.getAsDouble();				
			} else {
				this.lastPayAmountKroOld = -1d;				
			}
			
			if(modelF101 != null) {
				addToUpdateList(doChkPay(modelF101));
			}
			if(modelF201 != null) {
				addToUpdateList(doChkPay(modelF201));
			}
			
			LOG.debug("Worker end : ");
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private UpdateChkLstModel doChkPay(UpdateChkLstModel model) throws Exception {
		model.setStatus(StatusConstant.UPDATE_CHKPAY.getStatus());
		
		try {
			PaymentModel paymentInfo = null;
			int count = 0;
			while(true) {
				if(count == 5) {
					LOG.error("Cann't get paymentInfo");
					throw new CustomException(1, "Cann't get paymentInfo");
				}
				
				paymentInfo = KYSApi.getInstance().getPaymentInfo(this.proxy, this.sessionId, this.cif, model.getUri(), model.getLoanType(), model.getAccNo());					
				
				if(!paymentInfo.isError()) break;
				
				if(paymentInfo.isReFirstLogin()) {
					throw new CustomException(1, "Session have problem.");
				}
				
				LOG.warn(msgIndex + " Round[" + count + "] :=================: KYS Error :=============: sessionId " + this.sessionId);
				count++;
				Thread.sleep(5000);
			}
			
			if(paymentInfo.isRefresh()) {
				LOG.debug("Refresh Mode");
				return null;
			}
			
			Date lastPayDate = paymentInfo.getLastPayDate();
			if(lastPayDate != null) {
				double totalPayInstallment = paymentInfo.getTotalPayInstallment().doubleValue();
				double preBalance = paymentInfo.getPreBalance().doubleValue();
				double lastPayAmount = paymentInfo.getLastPayAmount().doubleValue();
				Date today = Calendar.getInstance().getTime();
				boolean isPaid = Boolean.FALSE;
				
				if(model.getLoanType().equals("F101")) {
					model.setLoanTypePay(model.getLoanType());
					
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
				} else if(model.getLoanType().equals("F201")) {
					model.setLoanTypePay(model.getLoanType());
					
					if(DateUtils.isSameDay(lastPayDate, today)) {
						if(this.lastPayAmountKroOld != lastPayAmount ||
								this.totalPayInstallmentKroOld.doubleValue() != totalPayInstallment ||
										this.preBalanceKroOld.doubleValue() != preBalance) {
							
							LOG.info("==================: Have Paid with option 1 :===================");
							isPaid = true;
						}
					} else if(this.lastPayDateKroOld == null) {
						LOG.info("==================: Have Paid with option 2 :===================");
						isPaid = true;					
					} else if(this.lastPayDateKroOld != null) {
						LocalDate lastPayDateOldLocalDate = this.lastPayDateKroOld.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
						LocalDate lastPayDateLocalDate = lastPayDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
						
						if(lastPayDateOldLocalDate.isBefore(lastPayDateLocalDate)) {
							LOG.info("==================: Have Paid with option 3 :===================");
							isPaid = true;
						}
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
			LOG.error(msgIndex + " [sessionId " + this.sessionId + "] ############## " + e.toString());
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
		
		model.setCreatedDateTime(new Date());
		LOG.info(msgIndex + " End checkpay " + model.getLoanType() + " " + model.getStatus());
		return model;
	}
	
}
