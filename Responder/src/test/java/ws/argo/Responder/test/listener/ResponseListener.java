package ws.argo.Responder.test.listener;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;

import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;


public class ResponseListener {

    private static int getPort(int defaultPort) {
        //grab port from environment, otherwise fall back to default port 9998
        String httpPort = System.getProperty("jersey.test.port");
        if (null != httpPort) {
            try {
                return Integer.parseInt(httpPort);
            } catch (NumberFormatException e) {
            }
        }
        return defaultPort;
    }

    private static URI getBaseURI() {
        return UriBuilder.fromUri("http://localhost/").port(getPort(9998)).build();
    }

    public static final URI BASE_URI = getBaseURI();
    
    public static HttpServer startServer() throws IOException {
        ResourceConfig resourceConfig = new PackagesResourceConfig("ws.argo.Responder.test.listener");

        System.out.println("Starting grizzly2...");
        HttpServer httpServer = GrizzlyServerFactory.createHttpServer(BASE_URI, resourceConfig);
        Map<HttpHandler, String[]> handlers = httpServer.getServerConfiguration().getHttpHandlers();
        
        Collection<String[]> values = handlers.values();
        
        return httpServer;
    }
    
    public static void main(String[] args) throws IOException {
        // Grizzly 2 initialization
        HttpServer httpServer = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...",
                BASE_URI));
        System.in.read();
        httpServer.stop();
    }    
}
