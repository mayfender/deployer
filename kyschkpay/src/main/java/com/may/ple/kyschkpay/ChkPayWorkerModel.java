package com.may.ple.kyschkpay;

import com.google.gson.JsonElement;

public class ChkPayWorkerModel {
	private String productId;
	private JsonElement jsonElement;
	private String contractNoColumnName;
	
	public ChkPayWorkerModel(String productId, JsonElement jsonElement, String contractNoColumnName) {
		this.productId = productId;
		this.jsonElement = jsonElement;
		this.contractNoColumnName = contractNoColumnName;
	}
	
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	public JsonElement getJsonElement() {
		return jsonElement;
	}
	public void setJsonElement(JsonElement jsonElement) {
		this.jsonElement = jsonElement;
	}
	public String getContractNoColumnName() {
		return contractNoColumnName;
	}
	public void setContractNoColumnName(String contractNoColumnName) {
		this.contractNoColumnName = contractNoColumnName;
	}
	
}
