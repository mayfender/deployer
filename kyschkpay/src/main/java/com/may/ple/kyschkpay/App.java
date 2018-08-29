package com.may.ple.kyschkpay;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class App {
	private static final Logger LOG = Logger.getLogger(App.class.getName());
	public static Map<String, JsonObject> auth = new HashMap<>();
	public static Properties prop;
	private static final int START_WORKING_HOUR = 5;
	private static final int END_WORKING_HOUR = 20;
	
	public static void main(String[] args) {
		InputStream input = null;
		
		try {
			LOG.info("Start Module...");
			System.setProperty("java.net.preferIPv4Stack", "true");
			
			//--[Read Properties file]
			input = new FileInputStream("./conf.properties");
			prop = new Properties();
			prop.load(input);
			input.close();
			
			List<String> prodIds = Arrays.asList(prop.getProperty("productIds").split(","));
			LOG.info("prodIds : " + prodIds);
			
			String jsonStr;
			for (String prodId : prodIds) {
				jsonStr = prop.getProperty(prodId);
				
				if(StringUtils.isBlank(jsonStr)) {
					auth.put(prodId, null);
					continue;
				}
				
				JsonElement jsEl =  new JsonParser().parse(jsonStr);
				JsonObject jsObj = jsEl.getAsJsonObject();
				auth.put(prodId, jsObj);
			}
			
			/*LOG.info("Start ManageLoginWorkerThread");
			new ManageLoginWorkerThread(prodIds).start();
			
			LOG.info("Start ManageCheckPayWorkerThread");
			new ManageCheckPayWorkerThread(prodIds).start();
			
			LOG.info("Start ManageExtWorkerThread");
			new ManageExtWorkerThread().start();*/
		} catch (Exception e) {
			LOG.error(e.toString());
		} finally {
			if (input != null) {
				try { input.close(); } catch (Exception e) {}
			}
		}
	}
	
	public static boolean checkWorkingHour() {
		int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		return hourOfDay >= App.START_WORKING_HOUR && hourOfDay <= App.END_WORKING_HOUR;
	}
	
}