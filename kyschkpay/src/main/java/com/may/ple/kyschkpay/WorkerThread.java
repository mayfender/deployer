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
	private String productId;
	
	public WorkerThread(JsonElement element, String idCardNoColumnName, String birthDateColumnName, String productId) {
		this.element = element;
		this.idCardNoColumnName = idCardNoColumnName;
		this.birthDateColumnName = birthDateColumnName;
		this.productId = productId;
	}

	@Override
	public void run() {
		try {
			LOG.debug("Start Worker");
			
			JsonObject data = element.getAsJsonObject();
			String id = data.get("_id").getAsString();
			JsonObject taskDetailFull = data.get("taskDetailFull").getAsJsonObject();
			String idCard = taskDetailFull.get(this.idCardNoColumnName).getAsString();
			String birthDate = taskDetailFull.get(this.birthDateColumnName).getAsString();
			
			String sessionId = login(idCard, birthDate);
			proceed(sessionId, id);
			
			LOG.debug("Worker end");
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
	private String login(String idCard, String birthDate) throws Exception {
		try {
			LOG.debug("Start login");
			String sessionId = StatusConstant.LOGIN_FAIL.getStatus().toString();
			int errCount = 0;
			
			while(StatusConstant.LOGIN_FAIL.getStatus().toString().equals(sessionId) || 
					StatusConstant.SERVICE_UNAVAILABLE.getStatus().toString().equals(sessionId)) {
				
				if(errCount == 10) break;
				
				sessionId = KYSApi.getInstance().login(idCard, birthDate);
//				Mock status for testing
//				sessionId = StatusConstant.LOGIN_SUCCESS.getStatus().toString();
				
				if(StatusConstant.SERVICE_UNAVAILABLE.getStatus().toString().equals(sessionId)) {
					LOG.warn("Service Unavailable");
					break;
				} else if(StatusConstant.LOGIN_FAIL.getStatus().toString().equals(sessionId)) {
					LOG.warn("Login fail");
					errCount++;
					Thread.sleep(5000);
				}
			}
			return sessionId;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
	private void proceed(String sessionId, String id) throws Exception {
		try {
			LOG.debug("Start proceed " + sessionId);
			
			if(StatusConstant.SERVICE_UNAVAILABLE.getStatus().toString().equals(sessionId)) return;
			
			UpdateChkLstModel model = new UpdateChkLstModel();
			model.setProductId(productId);
			model.setId(id);
			
			if(StatusConstant.LOGIN_FAIL.getStatus().toString().equals(sessionId)) {
				model.setStatus(StatusConstant.LOGIN_FAIL.getStatus());
				
				DMSApi.getInstance().updateChkLst(model);
			} else {
				model.setStatus(StatusConstant.LOGIN_SUCCESS.getStatus());
				model.setPaidDateTime(Calendar.getInstance().getTime());
				
//				KYSApi.getInstance().getPaymentInfo(sessionId);
				DMSApi.getInstance().updateChkLst(model);
			}
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}
	
}
