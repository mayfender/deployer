package com.may.ple.kyschkpay;

import com.google.gson.JsonElement;

public class LoginAndChkWorkerModel {
	private String productId;
	private JsonElement jsonElement;
	private String idCardNoColumnName;
	private String birthDateColumnName;
	private String contractNoColumnName;
	
	public LoginAndChkWorkerModel(String productId, JsonElement jsonElement, String idCardNoColumnName, String birthDateColumnName, String contractNoColumnName) {
		this.productId = productId;
		this.jsonElement = jsonElement;
		this.idCardNoColumnName = idCardNoColumnName;
		this.birthDateColumnName = birthDateColumnName;
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
	public String getIdCardNoColumnName() {
		return idCardNoColumnName;
	}
	public void setIdCardNoColumnName(String idCardNoColumnName) {
		this.idCardNoColumnName = idCardNoColumnName;
	}
	public String getBirthDateColumnName() {
		return birthDateColumnName;
	}
	public void setBirthDateColumnName(String birthDateColumnName) {
		this.birthDateColumnName = birthDateColumnName;
	}

	public String getContractNoColumnName() {
		return contractNoColumnName;
	}

	public void setContractNoColumnName(String contractNoColumnName) {
		this.contractNoColumnName = contractNoColumnName;
	}
	
}
