package com.may.ple.kyschkpay;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class App {
	private static final String PRODUCT_ID = "58ad698b22fdcb9665a7499b";
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	
	public static void main(String[] args) {
		try {
			DMSApi dmsApi = DMSApi.getInstance();
			long timeInMillis = Calendar.getInstance().getTimeInMillis();
			
			System.out.println("call login " + String.format("%1$tH:%1$tm:%1$tS", Calendar.getInstance().getTime()));
			dmsApi.login(USERNAME, PASSWORD);
			
			System.out.println("Get check List");
			JsonObject chkList = dmsApi.getChkList(PRODUCT_ID, timeInMillis);
			String idCardNoColumnName = chkList.get("idCardNoColumnName").getAsString();
			String birthDateColumnName = chkList.get("birthDateColumnName").getAsString();
			System.out.println("Original : " + chkList);
			
			JsonElement checkList = chkList.get("checkList");
			if(checkList == null) return;
			
			JsonElement status1 = checkList.getAsJsonObject().get("1"); //--: Pending
//			JsonElement status2 = checkList.getAsJsonObject().get("2"); //--: Login error
//			JsonElement status3 = checkList.getAsJsonObject().get("3"); //--: Paid
			
			if(status1 != null) {
				JsonArray status1Lst = status1.getAsJsonArray();
				ExecutorService executor = Executors.newFixedThreadPool(1);
				Runnable worker;
				
				for (JsonElement element : status1Lst) {
					worker = new WorkerThread(element, idCardNoColumnName, birthDateColumnName);
					executor.execute(worker);
				}
				
				executor.shutdown();
				while (!executor.isTerminated()) {}
				System.out.println("Finished all threads");
			}
			/*if(status2 != null) {
				
			}
			if(status3 != null) {
				
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
}