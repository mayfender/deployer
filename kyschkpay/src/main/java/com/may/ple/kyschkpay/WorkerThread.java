package com.may.ple.kyschkpay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class WorkerThread implements Runnable {
	private JsonElement element;
	private String idCardNoColumnName;
	private String birthDateColumnName;
	
	public WorkerThread(JsonElement element, String idCardNoColumnName, String birthDateColumnName) {
		this.element = element;
		this.idCardNoColumnName = idCardNoColumnName;
		this.birthDateColumnName = birthDateColumnName;
	}

	public void run() {
		try {
			System.out.println("Worker start");
			
			JsonObject data = element.getAsJsonObject();
			JsonObject taskDetailFull = data.get("taskDetailFull").getAsJsonObject();
			String idCard = taskDetailFull.get(this.idCardNoColumnName).getAsString();
			String birthDate = taskDetailFull.get(this.birthDateColumnName).getAsString();
			
			String sessionId = "0";
			int errCount = 0;
			
			while(sessionId.equals("0")) {
				if(errCount == 3) break;
				
				sessionId = KYSApi.getInstance().login(idCard, birthDate);
				
				if(sessionId.equals("-1")) {
					System.out.println("Service Unavailable");
					Thread.sleep(5000);
					errCount++;
				} else if(sessionId.equals("0")) {
					System.out.println("Login fail");
					errCount++;
					Thread.sleep(5000);
				}
			}
			
			System.out.println("Worker end");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
