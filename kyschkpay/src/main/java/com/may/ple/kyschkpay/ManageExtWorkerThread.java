package com.may.ple.kyschkpay;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class ManageExtWorkerThread extends Thread {
	private static final Logger LOG = Logger.getLogger(ManageExtWorkerThread.class.getName());
	private static final int CORE_POOL_SIZE = 25;
	private static final int MAXIMUM_POOL_SIZE = 50;
	private static final int KEEP_ALIVE_TIME = 180;
	private static final int SERVER_PORT = 9001;
	
	@Override
	public void run() {
		try (
				ServerSocket serv = new ServerSocket(SERVER_PORT);
			) {
			
			ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
			Socket client;
			
			while(true) {
				client = serv.accept();
				executor.execute(new ShowPaymentInfoWorker(client));
			}
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
}
