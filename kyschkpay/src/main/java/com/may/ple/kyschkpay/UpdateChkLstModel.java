package com.may.ple.kyschkpay;

import java.util.Date;

public class UpdateChkLstModel {
	private String productId;
	private String id;
	private Integer status;
	private Date paidDateTime;
	
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
	public Date getPaidDateTime() {
		return paidDateTime;
	}
	public void setPaidDateTime(Date paidDateTime) {
		this.paidDateTime = paidDateTime;
	}
	
}
