package com.may.ple.jwebsocket;

import org.jwebsocket.api.WebSocketClientEvent;
import org.jwebsocket.api.WebSocketClientTokenListener;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.client.token.JWebSocketTokenClient;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.token.MapToken;
import org.jwebsocket.token.Token;

public class ClientSimple implements WebSocketClientTokenListener {
	
	public static void main(String[] args) {
		try {
			System.out.println("Start");
			
//			new websockete
			JWebSocketTokenClient client2 = new JWebSocketTokenClient();
			client2.addTokenClientListener(new ClientSimple());
			
			client2.open("ws://localhost:8787/jWebSocket/jWebSocket");
			
			Thread.sleep(3000);
			client2.login("user", "user");
			
			Thread.sleep(3000);
			MapToken token = new MapToken(JWebSocketServerConstants.NS_BASE + ".plugins.debtalert", "registerUser");
			token.setString("username", "Mayfender");
			
			client2.sendToken(token);
//			client2.broadcastText("Hello All");
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
		System.out.println("processPacket");
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
