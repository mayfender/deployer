package com.may.ple.jwebsocket;

import java.util.Map.Entry;
import java.util.Set;

import javolution.util.FastMap;

import org.apache.log4j.Logger;
import org.jwebsocket.api.PluginConfiguration;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.kit.PlugInResponse;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.plugins.TokenPlugIn;
import org.jwebsocket.token.Token;

public class DebtAlertPlugin extends TokenPlugIn {
	public static final String NS_CHAT = JWebSocketServerConstants.NS_BASE + ".plugins.debtalert";
	private static final Logger mLog = Logging.getLogger();
	private final static String TT_REGISTER = "registerUser";
	private final static String TT_SEND = "sent";
	private final static String CTT_USERNAME = "username";
	private final FastMap<String, String> mConnectUsers = new FastMap<String, String>().shared();
	
	public DebtAlertPlugin(PluginConfiguration aConfiguration) {
		super(aConfiguration);
		if(mLog.isDebugEnabled()) {
			mLog.debug("Instantiating DebtCommon plug-in...");
		}
		
		this.setNamespace(NS_CHAT);
		
		if(mLog.isInfoEnabled()) {
			mLog.info("DebtCommon plug-in successfully instantiated.");
		}
	}
	
	@Override
	public void processToken(PlugInResponse aResponse, WebSocketConnector aConnector, Token aToken) {
		try {
			if (NS_CHAT.equals(aToken.getNS())) {
				mLog.debug(aToken.getType());
				
				if (TT_REGISTER.equals(aToken.getType())) {
					String username = aToken.getString(CTT_USERNAME);
					String usernameDummy = username;
					int startCount = 2;
					while(true) {
						if(mConnectUsers.containsKey(usernameDummy)) {
							usernameDummy = username + "_" + startCount++;
						} else {
							mLog.debug("Add connection " + usernameDummy + ":" + aConnector.getId());
							mConnectUsers.put(usernameDummy, aConnector.getId());							
							break;
						}
					}
				} else if (TT_SEND.equals(aToken.getType())) {
					String username = aToken.getString(CTT_USERNAME);
					Set<Entry<String, String>> entrySet = mConnectUsers.entrySet();
					WebSocketConnector connector;
					WebSocketPacket packet;
					
					for (Entry<String, String> entry : entrySet) {
						if(entry.getKey().contains(username)) {
							connector = getConnector(entry.getValue());
							packet = getServer().tokenToPacket(aConnector, aToken);
							connector.sendPacket(packet);
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
			if(mConnectUsers.containsValue(aConnector.getId())) {
				Set<Entry<String, String>> entrySet = mConnectUsers.entrySet();
				for (Entry<String, String> entry : entrySet) {
					if(entry.getValue().equals(aConnector.getId())) {
						mConnectUsers.remove(entry.getKey());
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
