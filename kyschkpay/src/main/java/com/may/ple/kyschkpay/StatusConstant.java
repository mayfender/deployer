package com.may.ple.kyschkpay;

public enum StatusConstant {
	SERVICE_UNAVAILABLE(-1),
	PENDING(1),
	LOGIN_FAIL(2),
	LOGIN_SUCCESS(3),
	UPDATE_CHKPAY(4),
	UPDATE_CHKPAY_PAID(5);
	
	private Integer status;
	
	private StatusConstant(Integer status) {
		this.status = status;
	}
	
	public static StatusConstant findByStatus(Integer status) {
		StatusConstant[] values = StatusConstant.values();
		for (StatusConstant rolesConstant : values) {
			if(rolesConstant.getStatus().equals(status)) 
				return rolesConstant;
		}
		return null;
	}

	public Integer getStatus() {
		return status;
	}
	
}
