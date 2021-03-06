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

package ws.argo.responder.transport.sns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import ws.argo.responder.transport.AmazonSNSTransport;

/**
 * The ResponseListener is a client used in providing the REST API for the Argo
 * respondTo protocol.
 * 
 * @author jmsimpson
 *
 */
public class SNSListener {

  private static final Logger LOGGER = LogManager.getLogger(SNSListener.class.getName());

  private static final int DEFAULT_PORT = 4004;
  
  /**
   * Return the local base URI based on the localhost address.
   * 
   * @return the default URI
   */
  public static URI getLocalBaseURI() {
    InetAddress localAddr;
    String addr;
    try {
      localAddr = InetAddress.getLocalHost();
      addr = localAddr.getHostAddress();
    } catch (UnknownHostException e) {
      LOGGER.warn("Issues finding ip address of locahost.  Using string 'localhost' for listener address binding");
      addr = "localhost";
    }
    return UriBuilder.fromUri("http://" + addr + "/").port(DEFAULT_PORT).build();
  }

  /**
   * Start the ResponseListener client. This largely includes starting at
   * Grizzly 2 server.
   * 
   * @param uri the uri of the service
   * @param t the link to the transport associated with this listener
   * @return a new HttpServer
   * @throws IOException if something goes wrong creating the http server
   */
  public static HttpServer startServer(URI uri, AmazonSNSTransport t) throws IOException {
    
    ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.registerInstances(new SNSListenerResource(t));
    resourceConfig.setApplicationName("Argo AmazonSNSTransport");
    
    
//    ResourceConfig resourceConfig = new ResourceConfig().packages("ws.argo.responder.transport.sns");

    LOGGER.debug("Starting Jersey-Grizzly2 JAX-RS listener...");
    HttpServer httpServer =  GrizzlyHttpServerFactory.createHttpServer(uri, resourceConfig, false);
    httpServer.getServerConfiguration().setName("SNS Listener");
    httpServer.start();
    LOGGER.info("Started Jersey-Grizzly2 JAX-RS listener.");

    return httpServer;

  }

}
