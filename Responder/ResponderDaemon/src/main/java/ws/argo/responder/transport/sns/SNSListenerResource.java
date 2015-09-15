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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.amazonaws.services.sns.model.ConfirmSubscriptionResult;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ws.argo.responder.transport.AmazonSNSTransport;
import ws.argo.wireline.probe.ProbeParseException;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.XMLSerializer;
import ws.argo.wireline.response.ResponseParseException;


/**
 * This is the CL UI probe listener resource used by the JAX-RS container. It
 * provides the REST API for the "respondTo" URL used in the probes.
 * 
 * @author jmsimpson
 *
 */
@Path("/listener")
public class SNSListenerResource {

  private static final Logger LOGGER = Logger.getLogger(SNSListenerResource.class.getName());

  private AmazonSNSTransport snsTransport;
  
  public SNSListenerResource(AmazonSNSTransport snsTransport) {
    this.snsTransport = snsTransport;
    LOGGER.info("Hey You.");
  }


  
  /**
   * Inbound JSON responses get processed here.
   * 
   * @return some innocuous string
   * @throws ResponseParseException if the wireline payload is malformed
   */
  @POST
  @Path("/sns")
  @Consumes("text/plain")
  public String handleSNSMessage(String message) {

    String trimmedMessage = message.trim();
    char leadingChar = trimmedMessage.charAt(0);
    
    switch (leadingChar) {
      case '<' : processXML(trimmedMessage); break;
      case '{' : processJson(trimmedMessage); break;
      default:
        break;
    }
    
    System.out.println(message);


    
    
    
    
    return "got SNS message";
  }
  
  private void processJson(String trimmedMessage) {

    LOGGER.info("Processing JSON message");

    Gson gson = new Gson();

    JsonObject jsonMsg = gson.fromJson(trimmedMessage, JsonObject.class);
    
    JsonElement type = jsonMsg.get("Type");
    
    switch (type.getAsString()) {
      case "SubscriptionConfirmation" :
        JsonElement token = jsonMsg.get("Token");
        JsonElement arn = jsonMsg.get("TopicArn");
        handleSubscriptionConfirmation(arn.getAsString(), token.getAsString());
        break;
      default:
        break;
    }    
  }

  private void processXML(String probeMessage) {
    LOGGER.info("Processing XML Probe message");
    
    try {
      XMLSerializer serializer = new XMLSerializer();

      ProbeWrapper probe = serializer.unmarshal(probeMessage);

      snsTransport.getProcessor().processProbe(probe);

    } catch (ProbeParseException e) {
      LOGGER.log(Level.SEVERE, "Error parsing inbound probe payload.", e);
    }

    
  }

  private void handleSubscriptionConfirmation(String arn, String token) {
    ConfirmSubscriptionResult result = snsTransport.getSNSClient().confirmSubscription(arn, token);  
    LOGGER.info("Confirmed Subsription to [" + arn + "] : " + result.toString());
  }

}
