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

package ws.argo.responder.test.listener;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import ws.argo.responder.Responder;

/**
 * The ResponseListener is a client used in testing the Responder.
 * 
 * @author jmsimpson
 *
 */
public class ResponseListener {

  private static final Logger LOGGER = LogManager.getLogger(Responder.class.getName());

  private static int getPort(int defaultPort) {
    // grab port from environment, otherwise fall back to default port 9998
    String httpPort = System.getProperty("jersey.test.port");
    if (null != httpPort) {
      try {
        return Integer.parseInt(httpPort);
      } catch (NumberFormatException e) {
        LOGGER.info( "Error in port number format", e);
      }
    }
    return defaultPort;
  }

  private static URI getBaseURI() {
    return UriBuilder.fromUri("http://localhost/").port(getPort(9998)).build();
  }

  public static final URI BASE_URI = getBaseURI();

  /**
   * Start the ResponseListener client. This largely includes starting at
   * Grizzly 2 server.
   * 
   * @return a new HttpServer
   * @throws IOException if something goes wrong creating the http server
   */
  public static HttpServer startServer() throws IOException {
    ResourceConfig resourceConfig = new ResourceConfig().packages("ws.argo.responder.test.listener");

    LOGGER.debug("Starting Jersey-Grizzly2 JAX-RS listener...");
    HttpServer httpServer =  GrizzlyHttpServerFactory.createHttpServer(BASE_URI, resourceConfig);
    LOGGER.info("Started Jersey-Grizzly2 JAX-RS listener.");

        return httpServer;
  }

}
