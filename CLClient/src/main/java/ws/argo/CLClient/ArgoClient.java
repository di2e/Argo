package ws.argo.CLClient;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

import ws.argo.CLClient.listener.ResponseListener;
import ws.argo.CLClient.listener.ServiceInfoBean;
import ws.argo.ProbeGenerator.ProbeGenerator;

public class ArgoClient {

	private final static Logger	LOGGER	= Logger.getLogger(ProbeGenerator.class.getName());

	private static HttpServer	server;

	private static WebResource	target;

	private ProbeGenerator	    gen;

	private HttpServer	        httpServer;

	public ArgoClient() throws IOException {
		gen = new ProbeGenerator("230.0.0.1", 4003);


	}
	
	public void startUp() throws IOException {
		startListener();
		
	}

	private static URI getBaseURI() {
		return UriBuilder.fromUri("http://localhost/").port(getPort(9998)).build();
	}

	private static int getPort(int defaultPort) {
		// grab port from environment, otherwise fall back to default port 9998
		String httpPort = System.getProperty("argoClient.port");
		if (null != httpPort) {
			try {
				return Integer.parseInt(httpPort);
			} catch (NumberFormatException e) {}
		}
		return defaultPort;
	}

	public static final URI	BASE_URI	= getBaseURI();

	private void startListener() throws IOException {
		ResourceConfig resourceConfig = new PackagesResourceConfig("ws.argo.CLClient.listener");

		System.out.println("Starting grizzly2...");
		httpServer = GrizzlyServerFactory.createHttpServer(BASE_URI, resourceConfig);

		Client c = Client.create();
		target = c.resource(getBaseURI());
	}

	public static void main(String[] args) throws IOException {
		LOGGER.info("Starting Argo Command Line Client");

		ArgoClient client = new ArgoClient();

		client.startUp();
		
	}


}
