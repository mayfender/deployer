package com.may.ple.kyschkpay;

import java.net.Proxy;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

public class LoginWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(LoginWorker.class.getName());
	private LoginProxyWorker proxyWorker;
	private LoginWorkerModel loginModel;
	private StatusConstant loginStatus;
	private Proxy proxy;
	private String id;
	private String sessionId;
	private String cif;
	private String idCard;
	private String birthDate;
	private String msgIndex;
	
	public LoginWorker(LoginProxyWorker proxyWorker, Proxy proxy, LoginWorkerModel loginModel) {
		this.proxyWorker = proxyWorker;
		this.proxy = proxy;
		this.loginModel = loginModel;
		this.msgIndex = (proxy != null ? proxy.toString() : "No Proxy");
	}
	
	@Override
	public void run() {
		try {
			JsonObject data = loginModel.getJsonElement().getAsJsonObject();
			this.id = data.get("_id").getAsString();
			this.idCard = data.get(loginModel.getIdCardNoColumnName()).getAsString();
			this.birthDate = birthDateFormat(data.get(loginModel.getBirthDateColumnName()).getAsString());
			
			LoginRespModel resp = login(idCard, birthDate);				
			CheckRespModel chkResp;
			
			this.loginStatus = resp.getStatus();
			this.sessionId = resp.getSessionId();
			this.cif = resp.getCif();
			
			if(StatusConstant.LOGIN_SUCCESS == loginStatus) {
				List<String> params = KYSApi.getInstance().getParam(proxy, this.sessionId, this.cif);
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
			
			LOG.debug(msgIndex + " Worker end : " + idCard);
		} catch (Exception e) {
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setId(this.id);
			model.setProductId(loginModel.getProductId());
			model.setErrMsg(e.toString());
			model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
			model.setCreatedDateTime(new Date());
			
			proxyWorker.addToLoginList(model);
			LOG.error(e.toString());
		}
	}
	
	private LoginRespModel login(String idCard, String birthDate) throws Exception {
		try {
			LOG.debug(msgIndex + " Start login");
			StatusConstant loginStatus = StatusConstant.LOGIN_FAIL;
			LoginRespModel resp = null;
			int errCount = 0;
			
			while(StatusConstant.LOGIN_FAIL == loginStatus || StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
				
				if(errCount == 3) break;
				
				resp = KYSApi.getInstance().login(proxy, idCard, birthDate);
				loginStatus = resp.getStatus();
				
				if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
					LOG.warn(msgIndex + " Service Unavailable");
					break;
				} else if(StatusConstant.LOGIN_FAIL  == loginStatus) {
					LOG.warn(msgIndex + " Login fail : " + errCount);
					errCount++;
				} else {
					LOG.info(msgIndex + " Login Success");					
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
			LOG.debug(msgIndex + " Start proceed " + this.sessionId);
			
			if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
				LOG.debug("==============: SERVICE_UNAVAILABLE :==================");
				return;
			}
			
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setId(this.id);
			model.setProductId(loginModel.getProductId());
			
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
				model.setProxy(proxy != null ? proxy.address().toString() : null);
			}
			
			model.setCreatedDateTime(new Date());
			proxyWorker.addToLoginList(model);
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
