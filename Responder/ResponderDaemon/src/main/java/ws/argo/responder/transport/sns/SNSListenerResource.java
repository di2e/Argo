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

import java.net.URISyntaxException;
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
import ws.argo.responder.transport.TransportConfigException;
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
  }

  /**
   * Inbound JSON responses get processed here.
   * 
   * @param message the JSON SNS message
   * @return some innocuous string
   */
  @POST
  @Path("/sns")
  @Consumes("text/plain")
  public String handleSNSMessage(String message) {

    processJson(message);

    return "Argo Received SNS message";
  }

  private void processJson(String message) {

    LOGGER.info("Processing JSON message");

    Gson gson = new Gson();
    JsonObject jsonMsg = gson.fromJson(message, JsonObject.class);
    JsonElement type = jsonMsg.get("Type");

    JsonElement token;
    JsonElement arn;
    
    switch (type.getAsString()) {
      case "SubscriptionConfirmation":
        token = jsonMsg.get("Token");
        arn = jsonMsg.get("TopicArn");
        handleSubscriptionConfirmation(arn.getAsString(), token.getAsString());
        break;
      case "UnsubscribeConfirmation":
        token = jsonMsg.get("Token");
        arn = jsonMsg.get("TopicArn");
        handleUnsubscriptionConfirmation(arn.getAsString(), token.getAsString());
        break;
      case "Notification":
        JsonElement probeString = jsonMsg.get("Message");
        handleProbeMessage(probeString.getAsString());
        break;
      default:
        break;
    }
  }

  private void handleProbeMessage(String probeMessage) {
    LOGGER.info("Processing XML Probe message");
  
    try {
      XMLSerializer serializer = new XMLSerializer();
      ProbeWrapper probe = serializer.unmarshal(probeMessage);
      snsTransport.getProcessor().processProbe(probe);
  
    } catch (ProbeParseException e) {
      LOGGER.log(Level.SEVERE, "Error parsing inbound probe payload.", e);
    }
  }

  private void handleUnsubscriptionConfirmation(String asString, String asString2) {
    LOGGER.warning("Handling unsubscribe confirmation message.  Will resubscribe if not in shutdown.");
    
    try {
      snsTransport.subscribe();
    } catch (URISyntaxException | TransportConfigException e) {
      LOGGER.log(Level.SEVERE, "Error during re-subscribe from housekeeping unsubscribe.", e);
    }
    
  }

  private void handleSubscriptionConfirmation(String arn, String token) {
    ConfirmSubscriptionResult result = snsTransport.getSNSClient().confirmSubscription(arn, token);
    LOGGER.info("Confirmed Subsription to [" + arn + "] : " + result.toString());
  }

}
