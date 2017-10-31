package com.may.ple.kyschkpay;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class App {
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	
	public static void main(String[] args) {
		try {
			DMSApi dmsApi = DMSApi.getInstance();
			long timeInMillis = Calendar.getInstance().getTimeInMillis();
			
			System.out.println("call login " + String.format("%1$tH:%1$tm:%1$tS", Calendar.getInstance().getTime()));
			dmsApi.login(USERNAME, PASSWORD);
			
			System.out.println("Get check List");
//			String[] prodIds = "58ad698b22fdcb9665a7499b,588c27cd22fd35487370f533".split(",");
			String[] prodIds = "58ad698b22fdcb9665a7499b".split(",");
			ExecutorService executor;
			
			while(true) {
				executor = Executors.newFixedThreadPool(50);
				
				for (String prodId : prodIds) {
					JsonObject chkList = dmsApi.getChkList(prodId, timeInMillis);
					String idCardNoColumnName = chkList.get("idCardNoColumnName").getAsString();
					String birthDateColumnName = chkList.get("birthDateColumnName").getAsString();
					System.out.println("Original : " + chkList);
					
					JsonElement checkList = chkList.get("checkList");
					if(checkList == null) continue;
					
					JsonElement status1 = checkList.getAsJsonObject().get("1"); //--: Pending
					JsonElement status2 = checkList.getAsJsonObject().get("2"); //--: Login error
					JsonElement status3 = checkList.getAsJsonObject().get("3"); //--: Paid
					
					proceed(executor, status1, idCardNoColumnName, birthDateColumnName, prodId);
					proceed(executor, status2, idCardNoColumnName, birthDateColumnName, prodId);
					proceed(executor, status3, idCardNoColumnName, birthDateColumnName, prodId);
					
					executor.shutdown();
					while (!executor.isTerminated()) {}
					System.out.println("Finished all threads");
				}
				Thread.sleep(600000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void proceed(ExecutorService executor, JsonElement element, String idCardNoColumnName, String birthDateColumnName, String productId) {		
		try {
			if(element == null) return;
			
			JsonArray status1Lst = element.getAsJsonArray();
			Runnable worker;
			
			for (JsonElement el : status1Lst) {
				worker = new WorkerThread(el, idCardNoColumnName, birthDateColumnName, productId);
				executor.execute(worker);
			}
		} catch (Exception e) {
			throw e;
		}		
	}
	
}