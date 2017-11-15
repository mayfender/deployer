package com.may.ple.kyschkpay;

import java.util.Calendar;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class WorkerThread implements Runnable {
	private static final Logger LOG = Logger.getLogger(WorkerThread.class.getName());
	private JsonElement element;
	private String idCardNoColumnName;
	private String birthDateColumnName;
	private String idCard;
	private String birthDate;
	private String productId;
	private String sessionId;
	private StatusConstant loginStatus;
	private String cif;
	private String id;
	
	public WorkerThread(JsonElement element, String idCardNoColumnName, String birthDateColumnName, String productId) {
		this.element = element;
		this.idCardNoColumnName = idCardNoColumnName;
		this.birthDateColumnName = birthDateColumnName;
		this.productId = productId;
	}

	@Override
	public void run() {
		try {
			JsonObject data = element.getAsJsonObject();
			this.id = data.get("_id").getAsString();
			JsonObject taskDetailFull = data.get("taskDetailFull").getAsJsonObject();
			this.idCard = taskDetailFull.get(this.idCardNoColumnName).getAsString();
			this.birthDate = taskDetailFull.get(this.birthDateColumnName).getAsString();
			if(data.get("sessionId") != null) {
				this.sessionId = data.get("sessionId").getAsString();
				this.cif = data.get("cif").getAsString();				
			}
			
			CheckRespModel chkResp;
			while(true) {
				if(sessionId == null) {
					LoginRespModel resp = login(idCard, birthDate);				
					this.loginStatus = resp.getStatus();
					this.sessionId = resp.getSessionId();	
					this.cif = resp.getCif();
					
					if(StatusConstant.LOGIN_SUCCESS == loginStatus) {
						chkResp = checkPay();
						updateLoginStatus(this.id, chkResp);
					} else {
						updateLoginStatus(this.id, null);
					}
				} else {
					try {
						checkPay();
					} catch (CustomException e) {
						if(e.errCode == 1) this.sessionId = null;
						LOG.error(e.toString());
						continue;
					} catch (Exception e) {
						throw e;
					}
				}
				break;
			}
			
			LOG.info("Worker end : " + idCard);
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
	private LoginRespModel login(String idCard, String birthDate) throws Exception {
		try {
			LOG.debug("Start login");
			StatusConstant loginStatus = StatusConstant.LOGIN_FAIL;
			LoginRespModel resp = null;
			int errCount = 0;
			
			while(StatusConstant.LOGIN_FAIL == loginStatus || StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
				
				if(errCount == 10) break;
				
				resp = KYSApi.getInstance().login(idCard, birthDate);
				loginStatus = resp.getStatus();
				
				if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
					LOG.warn("Service Unavailable");
					break;
				} else if(StatusConstant.LOGIN_FAIL  == loginStatus) {
					LOG.warn("Login fail : " + errCount);
					errCount++;
					Thread.sleep(3000);
				} else {
					LOG.info("Login Success");					
				}
			}
			
			return resp;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private void updateLoginStatus(String id, CheckRespModel chkResp) throws Exception {
		try {
			LOG.debug("Start proceed " + this.sessionId);
			
			if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) return;
			
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setProductId(productId);
			model.setId(id);
			
			if(StatusConstant.LOGIN_FAIL == loginStatus) {
				model.setStatus(loginStatus.getStatus());
			} else {
				model.setStatus(StatusConstant.LOGIN_SUCCESS.getStatus());
				model.setPaidDateTime(Calendar.getInstance().getTime());
				model.setSessionId(this.sessionId);
				model.setCif(this.cif);
				model.setLoanType(chkResp.getLoanType());
				model.setFlag(chkResp.getFlag());
				model.setAccNo(chkResp.getAccNo());
				model.setUri(chkResp.getUri());
			}
			
			DMSApi.getInstance().updateChkLst(model);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private CheckRespModel checkPay() throws Exception {
		try {
			LOG.debug("Start check pay");
//			CheckRespModel resp = KYSApi.getInstance().getPaymentInfo(sessionId, this.cif);
			CheckRespModel resp = null;
			return resp;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
