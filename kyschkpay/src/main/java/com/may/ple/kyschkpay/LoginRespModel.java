package com.may.ple.kyschkpay;

public class LoginRespModel {
	private StatusConstant status;
	private String cif;
	private String sessionId;
	
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

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
}
