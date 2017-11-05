package com.may.ple.kyschkpay;

public class CustomException extends Exception {
	private static final long serialVersionUID = -2778152939502565089L;
	public int errCode;
	
	public CustomException(int errCode, String msg) {
		super(msg);
		this.errCode = errCode;
	}
	
}
