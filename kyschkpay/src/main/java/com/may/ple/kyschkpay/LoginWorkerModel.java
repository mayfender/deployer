package com.may.ple.kyschkpay;

import com.google.gson.JsonElement;

public class LoginWorkerModel {
	private String productId;
	private JsonElement jsonElement;
	private String idCardNoColumnName;
	private String birthDateColumnName;
	
	public LoginWorkerModel(String productId, JsonElement jsonElement, String idCardNoColumnName, String birthDateColumnName) {
		this.productId = productId;
		this.jsonElement = jsonElement;
		this.idCardNoColumnName = idCardNoColumnName;
		this.birthDateColumnName = birthDateColumnName;
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
	
}
