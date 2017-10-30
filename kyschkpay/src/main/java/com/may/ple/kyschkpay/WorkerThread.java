package com.may.ple.kyschkpay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class WorkerThread implements Runnable {
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
			System.out.println("Worker start");
			
			JsonObject data = element.getAsJsonObject();
			String id = data.get("_id").getAsString();
			JsonObject taskDetailFull = data.get("taskDetailFull").getAsJsonObject();
			String idCard = taskDetailFull.get(this.idCardNoColumnName).getAsString();
			String birthDate = taskDetailFull.get(this.birthDateColumnName).getAsString();
			
			String sessionId = "0";
			int errCount = 0;
			
			
			
			while(StatusConstant.LOGIN_FAIL.getStatus().toString().equals(sessionId) || 
					StatusConstant.SERVICE_UNAVAILABLE.getStatus().toString().equals(sessionId)) {
				
				if(errCount == 10) break;
				
				sessionId = KYSApi.getInstance().login(idCard, birthDate);
				
				if(sessionId.equals("-1")) {
					System.out.println("Service Unavailable");
					break;
				} else if(sessionId.equals("0")) {
					System.out.println("Login fail");
					errCount++;
					Thread.sleep(5000);
				}
			}
			
			proceed(sessionId, id);
			
			System.out.println("Worker end");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void proceed(String sessionId, String id) throws Exception {
		try {
			if(sessionId.equals("0")) {
				DMSApi.getInstance().updateChkLst(productId, id, StatusConstant.LOGIN_FAIL.getStatus());
			} else {
//				KYSApi.getInstance()
			}
		} catch (Exception e) {
			throw e;
		}
	}
	
}
