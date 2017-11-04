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
			
			List<String> prodIds = Arrays.asList(args[0].split(","));
			DMSApi dmsApi = DMSApi.getInstance();			
			dmsApi.login(USERNAME, PASSWORD);
			
			long timeInMillis = Calendar.getInstance().getTimeInMillis();
			ExecutorService executor;
			String idCardNoColumnName;
			String birthDateColumnName;
			JsonElement status1;
			JsonElement status2;
			JsonElement status3;
			JsonObject chkList;
			LOG.debug("prodIds : " + prodIds);
			
			socketApi();
			
			while(true) {
				executor = Executors.newFixedThreadPool(50);
				
				for (String prodId : prodIds) {
					chkList = dmsApi.getChkList(prodId, timeInMillis);
					idCardNoColumnName = chkList.get("idCardNoColumnName").getAsString();
					birthDateColumnName = chkList.get("birthDateColumnName").getAsString();
					LOG.debug("Original : " + chkList);
					
					JsonElement checkList = chkList.get("checkList");
					if(checkList == null) continue;
					
					status1 = checkList.getAsJsonObject().get("1"); //--: Pending
					status2 = checkList.getAsJsonObject().get("2"); //--: Login error
					status3 = checkList.getAsJsonObject().get("3"); //--: Paid
					
					proceed(executor, status1, idCardNoColumnName, birthDateColumnName, prodId);
					proceed(executor, status2, idCardNoColumnName, birthDateColumnName, prodId);
					proceed(executor, status3, idCardNoColumnName, birthDateColumnName, prodId);
					
					executor.shutdown();
					while (!executor.isTerminated()) {}
					LOG.debug("Finished all threads");
				}
				Thread.sleep(600000);
			}
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}
	
	private static void proceed(ExecutorService executor, JsonElement element, String idCardNoColumnName, String birthDateColumnName, String productId) {		
		try {
			LOG.info("Start proceed");
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