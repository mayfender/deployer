package com.may.ple.kyschkpay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
public class ShowPaymentInfoWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(ShowPaymentInfoWorker.class.getName());
	private Socket socket;
	
	public ShowPaymentInfoWorker(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try (
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			) {
			
			LOG.info("Start ShowPaymentInfo");
			JsonElement jsonElement =  new JsonParser().parse(reader.readLine());
			JsonObject jsonRead = jsonElement.getAsJsonObject();
			
			String accNo = jsonRead.get("accNo").getAsString();
			String loanType = jsonRead.get("loanType").getAsString();
			String cif = jsonRead.get("cif").getAsString();
			String uri = jsonRead.get("uri").getAsString();
			String sessionId = jsonRead.get("sessionId").getAsString();
			String proxySet = jsonRead.get("proxy").getAsString();
			String idCard = jsonRead.get("ID_CARD").getAsString();
			String birthDate = jsonRead.get("BIRTH_DATE").getAsString();
			Proxy proxy = null;
			
			if(StringUtils.isNotBlank(proxySet)) {
				LOG.info("proxy: " + proxySet);
				String[] proxyStr = proxySet.split(":");
				proxy = new Proxy(
					Proxy.Type.HTTP,
					InetSocketAddress.createUnresolved(proxyStr[0], Integer.parseInt(proxyStr[1]))
				);
			}
			
			JsonObject jsonWrite = new JsonObject();
			LoginRespModel loginResp;
			boolean isErr = false;
			String html = "";
			int round = 0;
			Document doc;
			
			while(true) {
				LOG.info("Call getPaymentInfoPage");
				doc = getPaymentInfoPage(proxy, uri, loanType, accNo, cif, sessionId);
				
				if(doc == null) {
					isErr = true;
					break;
				}
				
				Elements body = doc.select("body");
				String onload = body.get(0).attr("onload");
				
				if(StringUtils.isNoneBlank(onload) && onload.toLowerCase().contains("login")) {
					LOG.warn("Session Timeout");
					
					if(round == 1) {
						isErr = true;
						break;
					}
					
					LOG.info("Call reLogin");
					loginResp = reLogin(proxy, idCard, DateUtil.birthDateFormat(birthDate));
					if(StatusConstant.SERVICE_UNAVAILABLE == loginResp.getStatus() || StatusConstant.LOGIN_FAIL == loginResp.getStatus()) {
						isErr = true;
						break;
					} else {
						sessionId = loginResp.getSessionId();
						jsonWrite.addProperty("sessionId", sessionId);
						round++;
						continue;
					}
				} else {
					Elements bExit = doc.select("td input[name='bExit']");
					if(bExit != null && bExit.size() > 0) {
						LOG.debug("Remove button");
						bExit.get(0).parent().remove();
					}
					
					LOG.info("Get HTML");
					html = doc.html();
					LOG.debug("End getHtml");
					break;
				}
			}
			
			jsonWrite.addProperty("html", html);
			jsonWrite.addProperty("isErr", isErr);
			writer.println(jsonWrite.toString());
			
			LOG.info("Finished");
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}
	
	private Document getPaymentInfoPage(Proxy proxy, String uri, String loanType, String accNo, String cif, String sessionId) throws Exception {
		try {
			Document doc;
			int i = 0;
			
			while(true) {
				doc = KYSApi.getInstance().getPaymentInfoPage(proxy, uri, loanType, accNo, cif, sessionId);
				
				if(doc.select("title").get(0).html().toUpperCase().equals("ERROR")) {
					if(i == 10) return null;
					
					LOG.error("Got error and try again round: " + i);
					Thread.sleep(1000);
					i++;
				} else {
					return doc;
				}
			}
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}

	private LoginRespModel reLogin(Proxy proxy, String idCard, String birthDate) throws Exception {
		try {
			StatusConstant loginStatus = StatusConstant.LOGIN_FAIL;
			LoginRespModel loginResp = null;
			int errCount = 0;
			
			while(StatusConstant.LOGIN_FAIL == loginStatus || StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
				if(errCount == 10) break;
				
				loginResp = KYSApi.getInstance().login(proxy, idCard, birthDate, errCount);
				loginStatus = loginResp.getStatus();
				
				if(StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
					LOG.warn(" Service Unavailable");
					break;
				} else if(StatusConstant.LOGIN_FAIL  == loginStatus) {
					errCount++;
					Thread.sleep(1000);
				} else {
					LOG.info("Login Success");
				}
			}
			return loginResp;
		} catch (Exception e) {
			LOG.error(e.toString());
			throw e;
		}
	}

}
