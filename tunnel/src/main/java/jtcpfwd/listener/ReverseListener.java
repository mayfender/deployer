package jtcpfwd.listener;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import jtcpfwd.Module;
import jtcpfwd.destination.Destination;

public class ReverseListener extends Listener {

	public static final String SYNTAX = "Reverse@<destination>";

	public static final Class[] getRequiredClasses() {
		return new Class[] { Destination.class };
	}

	private final Destination destination;
	private Socket currentSocket;

	public ReverseListener(String rule) throws Exception {
		destination = Destination.lookupDestination(rule);
	}

	public Module[] getUsedModules() {
		return new Module[] { destination };
	}

	protected Socket tryAccept() throws IOException {
		try {
			final InetSocketAddress target = destination.getNextDestination();
			if (target == null)
				return null;
			Socket s = new Socket();
			currentSocket = s;
			s.connect(target);
			boolean ok = false;
			InputStream is = null;
			
			try {
				is = s.getInputStream();
				int round = 0;
				while(true) {
					// Client IP have changed : If router has been changed IP Address will be issue because socket cann't connect together.
					// So this while loop will be observe by [is.available()] if no will be sleep 5 sec and continue while(true) again.
					// if still no request until round = 12 or 1 minute will return to get new socket.
					
					if(round == 12) {
						System.out.println("round 12");
						return null;
					}
					
					if(is.available() == 0) {
						round++;
						Thread.sleep(5000);
						continue;
					}
					break;
				}
				
				int b = is.read();
				if (b == -1)
					return null;
				if (b != 42)
					throw new IOException("Invalid marker byte received");
				ok = true;
				return s;
			} catch (Exception e) {
				return null;
			} finally {
				if (!ok) s.close();
			}
		} catch (ConnectException ex) {
			// ignore;
			return null;
		} finally {
			currentSocket = null;
		}
	}

	protected void tryDispose() throws IOException {
		destination.dispose();
		if (currentSocket != null)
			currentSocket.close();
	}
	
	@Override
	public Socket getCurrentSocket() {
		return currentSocket;
	}
}
