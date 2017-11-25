package com.may.ple.kyschkpay;

import java.util.Date;

public class UpdateChkLstModel {
	private String productId;
	private String id;
	private Integer status;
	private String sessionId;
	private String cif;
	private String loanType;
	private String flag;
	private String accNo;
	private String uri;
	private String errMsg;
	private Date lastPayDate;
	private Double lastPayAmount;
	private Double totalPayInstallment;
	private Double preBalance;
	private Date createdDateTime;
	private String contractNo;
	
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public String getCif() {
		return cif;
	}
	public void setCif(String cif) {
		this.cif = cif;
	}
	public String getLoanType() {
		return loanType;
	}
	public void setLoanType(String loanType) {
		this.loanType = loanType;
	}
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
	public String getAccNo() {
		return accNo;
	}
	public void setAccNo(String accNo) {
		this.accNo = accNo;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getErrMsg() {
		return errMsg;
	}
	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
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
	public Date getCreatedDateTime() {
		return createdDateTime;
	}
	public void setCreatedDateTime(Date createdDateTime) {
		this.createdDateTime = createdDateTime;
	}
	public String getContractNo() {
		return contractNo;
	}
	public void setContractNo(String contractNo) {
		this.contractNo = contractNo;
	}
	
}
