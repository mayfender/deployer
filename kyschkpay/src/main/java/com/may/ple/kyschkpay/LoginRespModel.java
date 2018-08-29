package com.may.ple.kyschkpay;

public class LoginRespModel {
	private StatusConstant status;
	private String cif;
	
	public StatusConstant getStatus() {
		return status;
	}

	public void setStatus(StatusConstant status) {
		this.status = status;
	}

	public String getCif() {
		return cif;
	}

	public void setCif(String cif) {
		this.cif = cif;
	}
	
}
