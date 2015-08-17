/*
 * Copyright 2015 Jeff Simpson.
 *
 * Licensed under the MIT License, (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.argo.CLClient.listener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

/**
 * The ResponseListener is a client used in providing the REST API for the Argo
 * respondTo protocol.
 * 
 * @author jmsimpson
 *
 */
public class ResponseListener {

  private static final Logger LOGGER = Logger.getLogger(ResponseListener.class.getName());

  private static int getPort(int defaultPort) {
    // grab port from environment, otherwise fall back to default port 9998
    String httpPort = System.getProperty("jersey.test.port");
    if (null != httpPort) {
      try {
        return Integer.parseInt(httpPort);
      } catch (NumberFormatException e) {
        LOGGER.log(Level.INFO, "Error in port number format", e);
      }
    }
    return defaultPort;
  }

  private static URI getBaseURI() {
    InetAddress localAddr;
    String addr;
    try {
      localAddr = InetAddress.getLocalHost();
      addr = localAddr.getHostAddress();
    } catch (UnknownHostException e) {
      LOGGER.warning("Issues finding ip address of locahost.  Using string 'localhost' for listener address binding");
      addr = "localhost";
    }
    return UriBuilder.fromUri("http://" + addr + "/").port(getPort(9998)).build();
  }

  public static final URI BASE_URI = getBaseURI();

  /**
   * Start the ResponseListener client. This largely includes starting at
   * Grizzly 2 server.
   * 
   * @return a new HttpServer
   * @throws IOException if something goes wrong creating the http server
   */
  public static HttpServer startServer(URI listenerURI) throws IOException {
    ResourceConfig resourceConfig = new PackagesResourceConfig("ws.argo.CLClient.listener");

    LOGGER.finer("Starting Jersey-Grizzly2 JAX-RS listener...");
    HttpServer httpServer = GrizzlyServerFactory.createHttpServer(listenerURI, resourceConfig);
    LOGGER.info("Started Jersey-Grizzly2 JAX-RS listener.");

    return httpServer;
  }

}
