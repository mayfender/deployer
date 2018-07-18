package com.may.ple.jwebsocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.jwebsocket.api.WebSocketClientEvent;
import org.jwebsocket.api.WebSocketClientTokenListener;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.client.token.JWebSocketTokenClient;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.token.MapToken;
import org.jwebsocket.token.Token;
import org.jwebsocket.token.TokenFactory;

public class ClientSimple implements WebSocketClientTokenListener {
	static JWebSocketTokenClient client;
	
	public static void main(String[] args) {
		try {
			System.out.println("Start");
			
//			new websockete
			client = new JWebSocketTokenClient();
			client.addTokenClientListener(new ClientSimple());
			
			client.open("ws://localhost:8787/jWebSocket/jWebSocket");
			
			Thread.sleep(3000);
			client.login("user", "user");
			
			Thread.sleep(3000);
			MapToken token = new MapToken(JWebSocketServerConstants.NS_BASE + ".plugins.debtalert", "registerUser");
			token.setString("username", "JWebsocketServer");			
			client.sendToken(token);
			
			Thread.sleep(3000);
			token = new MapToken(JWebSocketServerConstants.NS_BASE + ".plugins.debtalert", "getUsers");
			client.sendToken(token);
			
			System.out.println("Start to shutdown");
			Thread.sleep(3000);
			client.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Finish");
	}

	@Override
	public void processOpening(WebSocketClientEvent aEvent) {
		System.out.println("processOpening");
	}

	@Override
	public void processOpened(WebSocketClientEvent aEvent) {
		System.out.println("processOpened");
	}

	@Override
	public void processPacket(WebSocketClientEvent aEvent, WebSocketPacket aPacket) {
		try {
			System.out.println("processPacket");
			Token aToken = TokenFactory.packetToToken("json", aPacket);
			
			if(aToken.getNS().equals(JWebSocketServerConstants.NS_BASE + ".plugins.debtalert") && aToken.getType().equals("getUsersResp")) {
				List<String> users = aToken.getList("users");
				for (String u : users) {
					System.out.println(u);
				}
				
				if(users.size() > 0) {
					Thread.sleep(3000);
					Map<String, Integer> mUser = new HashMap<>();
					mUser.put("sadmin", 1);
					
					FastMap<String, Object> map = new FastMap<String, Object>().shared();
					MapToken token = new MapToken(map);
					token.setMap("users", mUser);
					token.setNS(JWebSocketServerConstants.NS_BASE + ".plugins.debtalert");
					token.setType("alert");
					
					token.setMap(map);
					client.sendToken(token);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processClosed(WebSocketClientEvent aEvent) {
		System.out.println("processClosed");
	}

	@Override
	public void processReconnecting(WebSocketClientEvent aEvent) {
		System.out.println("processReconnecting");
	}

	@Override
	public void processToken(WebSocketClientEvent aEvent, Token aToken) {
		System.out.println("processToken");
	}

}
