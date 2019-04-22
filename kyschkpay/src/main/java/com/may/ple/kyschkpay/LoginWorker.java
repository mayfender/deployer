package com.may.ple.kyschkpay;

import java.net.Proxy;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class LoginWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(LoginWorker.class.getName());
	private Map<String, String> secondLoginPage;
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
	
	/*public static void main(String[] args) {
		try {
			Map<String, String> secondLoginPage = KYSApi.getInstance().firstLogin(null, "SLF8533448", "XLnoi4237*");
			
			if(secondLoginPage != null) {					
				String sessionId = secondLoginPage.get("sessionId");
				
				LoginRespModel secondLoginResp = KYSApi.getInstance().secondLogin(null, "1801300030411", "19/04/2528", secondLoginPage);
				if(StatusConstant.LOGIN_SUCCESS == secondLoginResp.getStatus()) {					
					List<List<String>> argsList = KYSApi.getInstance().getParam(null, sessionId, secondLoginResp.getCif());
					System.out.println();
				}
			} else {
				System.err.println("First login fail.");
			}
			
			System.out.println("finished");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	@Override
	public void run() {
		try {
			JsonObject data = loginModel.getJsonElement().getAsJsonObject();
			this.id = data.get("_id").getAsString();
			this.idCard = data.get(loginModel.getIdCardNoColumnName()).getAsString();
			
			String birthDateDummy = data.get(loginModel.getBirthDateColumnName()).getAsString();
			if(birthDateDummy == null || (birthDateDummy = birthDateDummy.trim()).length() != 6) {
				LOG.warn("Skip in case of birthdate wrong format : " + birthDateDummy);
				return;
			}
			
			this.birthDate = DateUtil.birthDateFormat(birthDateDummy);
			
			LOG.debug("Call first login.");
			JsonObject auth = App.auth.get(loginModel.getProductId());
			JsonArray kysAcc = auth.getAsJsonArray("kys");
			secondLoginPage = doFirstLogin(kysAcc, proxy);
			if(secondLoginPage == null) {
				LOG.error("### 1st login ERROR!.");
				return;
			}
			
			LOG.debug("Call second login.");
			LoginRespModel resp = KYSApi.getInstance().secondLogin(proxy, idCard, birthDate, secondLoginPage);
			
			this.loginStatus = resp.getStatus();
			this.sessionId = secondLoginPage.get("sessionId");
			this.cif = resp.getCif();
			
			if(StatusConstant.LOGIN_SUCCESS == this.loginStatus) {
				List<List<String>> argsList = KYSApi.getInstance().getParam(this.proxy, this.sessionId, this.cif);
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
					addToUpdateList(chkResp);
				}				
			} else {
				addToUpdateList(null);
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
	
	private Map<String, String> doFirstLogin(JsonArray acc, Proxy proxy) throws Exception {
		try {
			Map<String, String> secondLoginPage = null;
			int round = 0;
			
			while(secondLoginPage == null) {
				if(round == 5) break;
				LOG.info("Do 1st login round: " + round);
				
				LOG.info("Call firstLogin");
				secondLoginPage = KYSApi.getInstance().firstLogin(proxy, acc.get(0).getAsString(), acc.get(1).getAsString());
				if(secondLoginPage == null) {
					LOG.warn("1st login fail.");
					Thread.sleep(1000);									
				} else {
					secondLoginPage.put("username", acc.get(0).getAsString());
					secondLoginPage.put("password", acc.get(1).getAsString());
				}
				round++;
			}
			
			LOG.info(secondLoginPage != null ? "1st login SUCCESS" : "1st login FAIL");
			return secondLoginPage;
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
			model.setProductId(this.loginModel.getProductId());
			
			if(StatusConstant.LOGIN_FAIL == this.loginStatus) {
				model.setStatus(this.loginStatus.getStatus());
			} else {
				model.setStatus(StatusConstant.LOGIN_SUCCESS.getStatus());
				model.setSessionId(this.sessionId);
				model.setCif(this.cif);
				model.setLoanType(chkResp.getLoanType());
				model.setFlag(chkResp.getFlag());
				model.setAccNo(chkResp.getAccNo());
				model.setUri(chkResp.getUri());
				model.setProxy(this.proxy != null ? this.proxy.address().toString() : null);
			}
			
			model.setCreatedDateTime(new Date());
			proxyWorker.addToLoginList(model);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
