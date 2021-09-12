package com.may.ple.dmsreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;


public class App {
	private static final Logger LOG = Logger.getLogger(App.class.getName());
	private Properties props;

	public static void main(String[] args) throws Exception {
		try {
			App app = new App();

			//---[1]
			app.resourceInit();
			LOG.info("Start App.");

			//---[2]
			app.serverInit();

			LOG.info("End App.");
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
	}

	private void resourceInit() throws Exception {
		String confPath = new File("conf").getAbsolutePath();

		//---[]
		PropertyConfigurator.configure(confPath + "/log4j.properties");

		//---[]
		props = new Properties();
		props.load(new FileInputStream(confPath + "/application.properties"));
	}

	private void serverInit() throws Exception {
		LOG.info("Start Server");
		Tomcat tomcat = new Tomcat();
        tomcat.setPort(Integer.valueOf(props.get("port").toString()));

        String webappPath = new File("webapp").getAbsolutePath();
        Context context = tomcat.addWebapp("/", webappPath);
        LOG.info("[webappPath] = " + webappPath);

        Tomcat.addServlet(context, "jersey-container-servlet", resourceConfig());
        Tomcat.addServlet(context, "servlet", new HttpServlet() {
			private static final long serialVersionUID = 723559607825571704L;

			@Override
            protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

                Writer w = resp.getWriter();
                w.write("Embedded Tomcat servlet.\n");
                w.flush();
                w.close();
            }
        });

        context.addServletMappingDecoded("/jersey/*", "jersey-container-servlet");
        context.addServletMappingDecoded("/servlet/*", "servlet");

        // Trigger the creation of the default connector for tomcat 9, tomcat 8 no need.
        tomcat.getConnector();

        tomcat.start();
        tomcat.getServer().await();

        LOG.info("End Server");
	}

	private ServletContainer resourceConfig() {
        return new ServletContainer(new ResourceConfig(new ResourceLoader().getClasses()));
    }








	/*private void init() throws IOException {
		listenAddress = new InetSocketAddress(PORT);

		this.selector = Selector.open();
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// bind server socket channel to port
		serverChannel.socket().bind(listenAddress);
		serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

		System.out.println("Server started on port >> " + PORT);

		Iterator<SelectionKey> iterator;
		Set<SelectionKey> readyKeys;
		SelectionKey key;
		int readyCount;

		while (true) {
			// wait for events
			readyCount = selector.select();
			if (readyCount == 0) {
				continue;
			}

			// process selected keys...
			readyKeys = selector.selectedKeys();
			iterator = readyKeys.iterator();

			while (iterator.hasNext()) {
				key = (SelectionKey) iterator.next();

				// Remove key from set so we don't process it twice
				iterator.remove();

				if (!key.isValid()) {
					continue;
				}

				if (key.isAcceptable()) { // Accept client connections
					this.accept(key);
				} else if (key.isReadable()) { // Read from client
					this.read(key);
				} else if (key.isWritable()) {
					// write data to client...
				}
			}
		}
	}

	// accept client connection
	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = serverChannel.accept();
		channel.configureBlocking(false);
		Socket socket = channel.socket();
		SocketAddress remoteAddr = socket.getRemoteSocketAddress();
		System.out.println("Connected to: " + remoteAddr);


		 * Register channel with selector for further IO (record it for read/write
		 * operations, here we have used read operation)

		channel.register(this.selector, SelectionKey.OP_READ);
	}

	// read from the socket channel
	private void read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		int numRead = -1;
		numRead = channel.read(buffer);

		if (numRead == -1) {
			Socket socket = channel.socket();
			SocketAddress remoteAddr = socket.getRemoteSocketAddress();
			System.out.println("Connection closed by client: " + remoteAddr.hashCode());
			channel.close();
			key.cancel();
			return;
		}

		String result = new String(buffer.array(), "utf8").trim();
		System.out.println("from: " + channel.getRemoteAddress().hashCode() + ", Got: " + result);

//		byte[] data = new byte[numRead];
//		System.arraycopy(buffer.array(), 0, data, 0, numRead);
//		System.out.println("from: " + channel.getRemoteAddress().toString() + ", Got: " + new String(data));

	}*/

}
