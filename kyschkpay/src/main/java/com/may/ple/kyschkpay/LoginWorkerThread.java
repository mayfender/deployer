package com.may.ple.kyschkpay;

import java.util.Date;
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
	private String productId;
	
	public LoginWorkerThread(String productId, JsonElement element, String idCardNoColumnName, String birthDateColumnName) {
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
			this.idCard = data.get(this.idCardNoColumnName).getAsString();
			this.birthDate = birthDateFormat(data.get(this.birthDateColumnName).getAsString());
			
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
				
				addToUpdateList(chkResp);
			} else {
				addToUpdateList(null);
			}
			
			LOG.info("Worker end : " + idCard);
		} catch (Exception e) {
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setId(this.id);
			model.setErrMsg(e.toString());
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			model.setCreatedDateTime(new Date());
			
			App.loginWorker.addToLoginList(model, productId);
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
				
				if(errCount == 3) break;
				
				resp = KYSApi.getInstance().login(idCard, birthDate);
				loginStatus = resp.getStatus();
				
				if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
					LOG.warn("Service Unavailable");
					break;
				} else if(StatusConstant.LOGIN_FAIL  == loginStatus) {
					LOG.warn("Login fail : " + errCount);
					errCount++;
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
	
	private void addToUpdateList(CheckRespModel chkResp) throws Exception {
		try {
			LOG.debug("Start proceed " + this.sessionId);
			
			if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
				LOG.debug("==============: SERVICE_UNAVAILABLE :==================");
				return;
			}
			
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setId(this.id);
			
			if(StatusConstant.LOGIN_FAIL == loginStatus) {
				model.setStatus(loginStatus.getStatus());
			} else {
				model.setStatus(StatusConstant.LOGIN_SUCCESS.getStatus());
				model.setSessionId(this.sessionId);
				model.setCif(this.cif);
				model.setLoanType(chkResp.getLoanType());
				model.setFlag(chkResp.getFlag());
				model.setAccNo(chkResp.getAccNo());
				model.setUri(chkResp.getUri());
			}
			
			model.setCreatedDateTime(new Date());
			App.loginWorker.addToLoginList(model, this.productId);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private static String birthDateFormat(String str) {
		if(str.contains("/")) {
			return str;
		}
		
		String day = str.substring(0, 2);
		String month = str.substring(2, 4);
		String year = str.substring(4);
		return day + "/" + month + "/" + year;
	}
	
}
