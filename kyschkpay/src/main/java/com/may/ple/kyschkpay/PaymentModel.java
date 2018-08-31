package com.may.ple.kyschkpay;

import java.util.Date;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.jsoup.nodes.Document;

public class PaymentModel {
	private Date lastPayDate;
	private Double lastPayAmount;
	private Double totalPayInstallment;
	private Double preBalance;
	private boolean isRefresh;
	private boolean isError;
	private Document doc;
	private boolean isReFirstLogin;
	private String sessionId;
	
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toStringExclude(this, "doc");
	}
	
	public Date getLastPayDate() {
		return lastPayDate;
	}
	public void setLastPayDate(Date lastPayDate) {
		this.lastPayDate = lastPayDate;
	}
	public Double getLastPayAmount() {
		return lastPayAmount;
	}
	public void setLastPayAmount(Double lastPayAmount) {
		this.lastPayAmount = lastPayAmount;
	}
	public Double getTotalPayInstallment() {
		return totalPayInstallment;
	}
	public void setTotalPayInstallment(Double totalPayInstallment) {
		this.totalPayInstallment = totalPayInstallment;
	}
	public Double getPreBalance() {
		return preBalance;
	}
	public void setPreBalance(Double preBalance) {
		this.preBalance = preBalance;
	}
	public boolean isRefresh() {
		return isRefresh;
	}
	public void setRefresh(boolean isRefresh) {
		this.isRefresh = isRefresh;
	}
	public boolean isError() {
		return isError;
	}
	public void setError(boolean isError) {
		this.isError = isError;
	}
	public Document getDoc() {
		return doc;
	}
	public void setDoc(Document doc) {
		this.doc = doc;
	}

	public boolean isReFirstLogin() {
		return isReFirstLogin;
	}

	public void setReFirstLogin(boolean isReFirstLogin) {
		this.isReFirstLogin = isReFirstLogin;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
}
