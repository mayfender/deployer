package com.may.ple.kyschkpay;

import java.util.Date;

public class PaymentModel {
	private Date lastPayDate;
	private Double lastPayAmount;
	private Double totalPayInstallment;
	private Double preBalance;
	private boolean isRefresh;
	private boolean isError;
	private String html;
	
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
	public String getHtml() {
		return html;
	}
	public void setHtml(String html) {
		this.html = html;
	}
	
}
