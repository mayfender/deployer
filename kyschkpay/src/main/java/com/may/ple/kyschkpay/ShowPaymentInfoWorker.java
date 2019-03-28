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
//	private Entry<String, Map<String, String>> dummy;
	
	public ShowPaymentInfoWorker(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try (
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			) {
			
			//--: TODO: Should be fixed to get by key format: productId:proxy:loanType
			/*dummy = ManageLoginWorkerThread.firstLoginMap.entrySet().iterator().next();
			String[] keyArr = dummy.getKey().split("#");
			String proxySet = keyArr[1];
			if(proxySet.equals("NOPROXY")) {
				proxySet = null;
			}*/
			//--
			
			LOG.info("Start ShowPaymentInfo");
			JsonElement jsonElement =  new JsonParser().parse(reader.readLine());
			JsonObject jsonRead = jsonElement.getAsJsonObject();
			
			String accNo = jsonRead.get("accNo").getAsString();
			String loanType = jsonRead.get("loanType").getAsString();
			String cif = jsonRead.get("cif").getAsString();
			String uri = jsonRead.get("uri").getAsString();
			String sessionId = jsonRead.get("sessionId").getAsString();
			String proxySet = jsonRead.get("proxy").getAsString();
			/*String idCard = jsonRead.get("ID_CARD").getAsString();
			String birthDate = jsonRead.get("BIRTH_DATE").getAsString();*/
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
//			LoginRespModel loginResp;
			boolean isErr = false;
			String html = "";
//			int round = 0;
			Document doc = null;
			
			if(StringUtils.isBlank(sessionId)) {
				throw new Exception("sessionId is blank.");
			}
			
			LOG.info("Call getPaymentInfoPage");
			doc = getPaymentInfoPage(proxy, uri, loanType, accNo, cif, sessionId);
			
			if(doc == null) {
				throw new Exception("Cannot get getPaymentInfoPage");
			}
						
			Elements bExit = doc.select("td input[name='bExit']");
			if(bExit != null && bExit.size() > 0) {
				LOG.debug("Remove button");
				bExit.get(0).parent().remove();
			}
			
			LOG.info("Get HTML");
			html = doc.html();
			LOG.debug("End getHtml");
			
			
			
			
			/*while(true) {
				if(StringUtils.isNotBlank(sessionId)) {
					LOG.info("Call getPaymentInfoPage");
					doc = getPaymentInfoPage(proxy, uri, loanType, accNo, cif, sessionId);
					
					if(doc == null) {
						isErr = true;
						break;
					}
					
					Elements body = doc.select("body");
					onload = body.get(0).attr("onload");
				}
				
				if(StringUtils.isNoneBlank(onload) && onload.toLowerCase().contains("login") || StringUtils.isBlank(sessionId)) {
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
						List<List<String>> argsList = KYSApi.getInstance().getParam(proxy, sessionId, loginResp.getCif());
						
						cif = loginResp.getCif();
						jsonWrite.addProperty("cif", cif);
						jsonWrite.addProperty("sessionId", sessionId);
						
						for (List<String> params : argsList) {
							loanType = params.get(0);
							accNo = params.get(2);
							
							if(params.get(5).equals("1")) {
								uri = KYSApi.LINK + "/STUDENT/ESLMTI001.do";
							} else {
								uri = KYSApi.LINK + "/STUDENT/ESLMTI003.do";
							}
							
							if(loanType.equals("F101")) {	
								jsonWrite.addProperty("loanType", loanType);
								jsonWrite.addProperty("flag", params.get(5));
								jsonWrite.addProperty("accNo", accNo);
								jsonWrite.addProperty("uri", uri);
							} else if(loanType.equals("F201")) {
								jsonWrite.addProperty("loanType_kro", loanType);
								jsonWrite.addProperty("flag_kro", params.get(5));
								jsonWrite.addProperty("accNo_kro", accNo);
								jsonWrite.addProperty("uri_kro", uri);
							}
						}
						
						if(jsonWrite.get("loanType") != null) {							
							loanType = jsonWrite.get("loanType").getAsString();
							accNo = jsonWrite.get("accNo").getAsString();
							uri = jsonWrite.get("uri").getAsString();
						} else if(jsonWrite.get("loanType_kro") != null) {
							loanType = jsonWrite.get("loanType_kro").getAsString();
							accNo = jsonWrite.get("accNo_kro").getAsString();
							uri = jsonWrite.get("uri_kro").getAsString();
						}
						
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
			}*/
			
			jsonWrite.addProperty("html", html);
			jsonWrite.addProperty("isErr", isErr);
			writer.println(jsonWrite.toString());
			
			LOG.info("Finished");
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		} finally {
			try {
				if(socket != null) socket.close();				
			} catch (Exception e2) {}
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

	/*private LoginRespModel reLogin(Proxy proxy, String idCard, String birthDate) throws Exception {
		try {
			StatusConstant loginStatus = StatusConstant.LOGIN_FAIL;
			LoginRespModel loginResp = null;
			int errCount = 0;
			
			while(StatusConstant.LOGIN_FAIL == loginStatus || StatusConstant.SERVICE_UNAVAILABLE == loginStatus) {
				if(errCount == 3) break;
				
				loginResp = KYSApi.getInstance().secondLogin(proxy, idCard, birthDate, dummy.getValue());
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
	}*/

}
