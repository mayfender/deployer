package com.may.ple.kyschkpay;

import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LoginWorkerThread implements Runnable {
	private static final Logger LOG = Logger.getLogger(LoginWorkerThread.class.getName());
	private JsonElement element;
	private String idCardNoColumnName;
	private String birthDateColumnName;
	private String idCard;
	private String birthDate;
	private String sessionId;
	private StatusConstant loginStatus;
	private String cif;
	private String id;
	
	public LoginWorkerThread(JsonElement element, String idCardNoColumnName, String birthDateColumnName) {
		this.element = element;
		this.idCardNoColumnName = idCardNoColumnName;
		this.birthDateColumnName = birthDateColumnName;
	}

	@Override
	public void run() {
		try {
			JsonObject data = element.getAsJsonObject();
			this.id = data.get("_id").getAsString();
			JsonObject taskDetailFull = data.get("taskDetailFull").getAsJsonObject();
			this.idCard = taskDetailFull.get(this.idCardNoColumnName).getAsString();
			this.birthDate = taskDetailFull.get(this.birthDateColumnName).getAsString();
			
			LoginRespModel resp = login(idCard, birthDate);				
			CheckRespModel chkResp;
			
			this.loginStatus = resp.getStatus();
			this.sessionId = resp.getSessionId();	
			this.cif = resp.getCif();
			
			if(StatusConstant.LOGIN_SUCCESS == loginStatus) {
				List<String> params = KYSApi.getInstance().getParam(this.sessionId, this.cif);
				chkResp = new CheckRespModel();
				chkResp.setLoanType(params.get(0).trim());
				chkResp.setFlag(params.get(5).trim());
				chkResp.setAccNo(params.get(2).trim());
				
				if(chkResp.getFlag().equals("1")) {
					chkResp.setUri(KYSApi.LINK + "/STUDENT/ESLMTI001.do");
				} else {
					chkResp.setUri(KYSApi.LINK + "/STUDENT/ESLMTI003.do");
				}
				
				addToUpdateList(this.id, chkResp);
			} else {
				addToUpdateList(this.id, null);
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
				
				if(errCount == 5) break;
				
				resp = KYSApi.getInstance().login(idCard, birthDate);
				loginStatus = resp.getStatus();
				
				if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
					LOG.warn("Service Unavailable");
					break;
				} else if(StatusConstant.LOGIN_FAIL  == loginStatus) {
					LOG.warn("Login fail : " + errCount);
					errCount++;
					Thread.sleep(1000);
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
	
	private void addToUpdateList(String id, CheckRespModel chkResp) throws Exception {
		try {
			LOG.debug("Start proceed " + this.sessionId);
			
			if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) return;
			
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setId(id);
			
			if(StatusConstant.LOGIN_FAIL == loginStatus) {
				model.setStatus(loginStatus.getStatus());
				
				ManageLoginWorkerThread.addToLoginFailList(model);
			} else {
				model.setStatus(StatusConstant.LOGIN_SUCCESS.getStatus());
				model.setPaidDateTime(Calendar.getInstance().getTime());
				model.setSessionId(this.sessionId);
				model.setCif(this.cif);
				model.setLoanType(chkResp.getLoanType());
				model.setFlag(chkResp.getFlag());
				model.setAccNo(chkResp.getAccNo());
				model.setUri(chkResp.getUri());
				
				ManageLoginWorkerThread.addToLoginSuccessList(model);
			}
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
