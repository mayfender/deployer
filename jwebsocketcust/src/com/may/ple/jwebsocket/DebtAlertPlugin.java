package com.may.ple.jwebsocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javolution.util.FastMap;

import org.apache.log4j.Logger;
import org.jwebsocket.api.PluginConfiguration;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.kit.PlugInResponse;
import org.jwebsocket.plugins.TokenPlugIn;
import org.jwebsocket.token.Token;
import org.jwebsocket.token.TokenFactory;

public class DebtAlertPlugin extends TokenPlugIn {
	private static final Logger mLog = Logger.getLogger(DebtAlertPlugin.class);
	public static final String NS_DEBTALERT = JWebSocketServerConstants.NS_BASE + ".plugins.debtalert";
	private final static String TT_REGISTER = "registerUser";
	private final static String TT_ALERT = "alert";
	private final static String TT_GET_USERS = "getUsers";
	private final static String TT_GET_USERS_RESP = "getUsersResp";
	private final static String CTT_USERNAME = "username";
	private final static String CTT_USERS = "users";
	private final static String CTT_ALERT_NUM = "alertNum";
	private final FastMap<String, String> mConntU = new FastMap<String, String>().shared();
	
	public DebtAlertPlugin(PluginConfiguration aConfiguration) {
		super(aConfiguration);
		if(mLog.isDebugEnabled()) {
			mLog.debug("Instantiating DebtAlertPlugin plug-in...");
		}
		
		this.setNamespace(NS_DEBTALERT);
		
		if(mLog.isInfoEnabled()) {
			mLog.info("DebtAlertPlugin plug-in successfully instantiated.");
		}
	}
	
	@Override
	public void processToken(PlugInResponse aResponse, WebSocketConnector aConnector, Token aToken) {
		try {
			if (NS_DEBTALERT.equals(aToken.getNS())) {
				mLog.debug(aToken.getType());
				
				if (TT_REGISTER.equals(aToken.getType())) {
					String username = aToken.getString(CTT_USERNAME);
					String usernameDummy = username;
					int startCount = 2;
					while(true) {
						if(mConntU.containsKey(usernameDummy)) {
							usernameDummy = username + "_" + (startCount++) + "@#&";
						} else {
							mLog.debug("Add connection " + usernameDummy + ":" + aConnector.getId());
							mConntU.put(usernameDummy, aConnector.getId());							
							break;
						}
					}
				} else if (TT_GET_USERS.equals(aToken.getType())) {
					List<String> lUname = new ArrayList<>();
					Set<Entry<String, String>> conntUSet = mConntU.entrySet();
					
					for (Entry<String, String> conntEntry : conntUSet) {
						if(conntEntry.getKey().contains("@#&") || conntEntry.getKey().contains("JWebsocketServer")) continue;
						lUname.add(conntEntry.getKey());
					}
					
					Token lToken = TokenFactory.createToken(getNamespace(), TT_GET_USERS_RESP);
					lToken.setList(CTT_USERS, lUname);
					getServer().sendToken(aConnector, lToken);
				} else if (TT_ALERT.equals(aToken.getType())) {
					Map<String, Integer> mUser = aToken.getMap(CTT_USERS);
					Set<Entry<String, Integer>> uSet = mUser.entrySet();
					Set<Entry<String, String>> conntUSet;
					WebSocketConnector connt;
					Token lToken;
					
					for (Entry<String, Integer> uEntry : uSet) {
						lToken = TokenFactory.createToken(getNamespace(), TT_ALERT);
						lToken.setInteger(CTT_ALERT_NUM, uEntry.getValue());
						conntUSet = mConntU.entrySet();
						
						for (Entry<String, String> conntEntry : conntUSet) {
							if(conntEntry.getKey().contains(uEntry.getKey())) {
								connt = getConnector(conntEntry.getValue());
								getServer().sendToken(connt, lToken);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			mLog.error(e.toString(), e);
		}
	}
	
	@Override
	public void connectorStopped(WebSocketConnector aConnector, CloseReason aCloseReason) {
		super.connectorStopped(aConnector, aCloseReason);
		try {
			if(mConntU.containsValue(aConnector.getId())) {
				Set<Entry<String, String>> entrySet = mConntU.entrySet();
				for (Entry<String, String> entry : entrySet) {
					if(entry.getValue().equals(aConnector.getId())) {
						mConntU.remove(entry.getKey());
						mLog.debug("Remove connection " + entry.getKey() + ":" + entry.getValue());
						break;
					}
				}				
			}
		} catch (Exception e) {
			mLog.error(e.toString(), e);
		}
	}

}
