package com.may.ple.kyschkpay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class App {
	private static final Logger LOG = Logger.getLogger(App.class.getName());
	private static final String USERNAME = "system";
	private static final String PASSWORD = "w,j[vd8iy[";
	
	//--: args[0]: Product ID[proId-1,proId-2]
	public static void main(String[] args) {
		try {
			LOG.info("Start Module...");
			
			if(args == null || args.length == 0) {
				LOG.error("args can't be null");
				return;
			}
			
			DMSApi dmsApi = DMSApi.getInstance();
			List<String> prodIds = Arrays.asList(args[0].split(","));
			LOG.info("prodIds : " + prodIds);
			

			
//			socketApi();
//			
			LOG.info("Start LoginWorkerThread");
			new LoginWorkerThread(prodIds).start();
			
			
			
			
			
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
	private void firstTimeLogin(JsonElement status) {
		DMSApi dmsApi = DMSApi.getInstance();
		int poolSize = 500;
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(poolSize);
		String idCardNoColumnName;
		String birthDateColumnName;
		JsonElement status1;
		JsonElement status2;
		JsonElement status3;
		JsonObject chkList;
		int currentPage = 1;
		int itemsPerPage = 50;
		boolean isSuccess;
		List<String> prodIds = null;
		int hourOfDay;
		
		try {
			while(true) {
				hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				LOG.info("hourOfDay : " + hourOfDay);
				if(!(hourOfDay >= 5 && hourOfDay <= 22)) {
					LOG.info("Sleep 30 min");
					Thread.sleep(1800000);
					continue;
				}
				
				isSuccess = dmsApi.login(USERNAME, PASSWORD);
				if(!isSuccess) {
					LOG.warn("May be server is down.");
					Thread.sleep(30000);
					continue;
				}
				
				for (String prodId : prodIds) {
					while(executor.getQueue().size() > (poolSize/2)) {
						LOG.info("Wait queue size to 0 now : " + executor.getQueue().size());						
						Thread.sleep(30000);
					}
					
					chkList = dmsApi.getChkList(prodId, currentPage, itemsPerPage);
					if(chkList == null) {
						LOG.warn("chkList is null may be server is down.");
						Thread.sleep(30000);
						continue;
					}
					
					idCardNoColumnName = chkList.get("idCardNoColumnName").getAsString();
					birthDateColumnName = chkList.get("birthDateColumnName").getAsString();
					LOG.debug("Original : " + chkList);
					
					JsonElement checkList = chkList.get("checkList");
					if(checkList == null) continue;
					
					status1 = checkList.getAsJsonObject().get("1"); //--: Pending
					status2 = checkList.getAsJsonObject().get("2"); //--: Login error
	//				status3 = checkList.getAsJsonObject().get("3"); //--: Paid
					
					proceed(executor, status1, idCardNoColumnName, birthDateColumnName, prodId);
					proceed(executor, status2, idCardNoColumnName, birthDateColumnName, prodId);
	//				proceed(executor, status3, idCardNoColumnName, birthDateColumnName, prodId);
				}
				
				Thread.sleep(600000);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private void failLogin(JsonElement status) {
		
	}
	
	private void chkPay(JsonElement status) {
		
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
			LOG.error(e.toString());
			throw e;
		}		
	}
	
	private static void socketApi() {
		LOG.info("Start socket api");
		new Thread() {
			public void run() {
				ServerSocket serverSock = null;
				
				try {
					serverSock = new ServerSocket(9001);	
					
					while(true) {
						Socket socket = serverSock.accept();
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						String read;
						while((read = reader.readLine())  != null) {
							if(read.equals("SHUTDOWN")) System.exit(0);
						}
						socket.close();
					}
				} catch (Exception e) {
					LOG.error(e.toString());
				} finally {
					try {
						if(serverSock != null) serverSock.close();						
					} catch (Exception e2) {}
				}
			}
		}.start();
	}
	
}