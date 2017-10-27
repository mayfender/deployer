package com.may.ple.kyschkpay;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
	
	public static void main(String[] args) {
		try {
			new ThreadPool().process();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void process() {
		/*ExecutorService executor = Executors.newFixedThreadPool(5);// creating a pool of 5 threads
		
		for (int i = 0; i < 10; i++) {
			Runnable worker = new WorkerThread("" + i);
			executor.execute(worker);// calling execute method of ExecutorService
		}
		System.out.println("execute all worker already.");
		
		executor.shutdown();
		while (!executor.isTerminated()) {}

		System.out.println("Finished all threads");*/
	}

}
