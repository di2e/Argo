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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.amazonaws.services.sns.model.ConfirmSubscriptionResult;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * This is the CL UI probe listener resource used by the JAX-RS container. It
 * provides the REST API for the "respondTo" URL used in the probes.
 * 
 * @author jmsimpson
 *
 */
@Path("/listener")
public class SNSListenerResource {

  /**
   * Inbound JSON responses get processed here.
   * 
   * @param probeResponseJSON - the actual wireline response payload
   * @return some innocuous string
   * @throws ResponseParseException if the wireline payload is malformed in some
   *           way
   */
  @POST
  @Path("/sns")
  @Consumes("text/plain")
  public String handleSNSMessage(String message) {

    System.out.println(message);

    Gson gson = new Gson();

    JsonObject jsonMsg = gson.fromJson(message, JsonObject.class);
    
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
    
    
    
    
    return "got SNS message";
  }
  
  private void handleSubscriptionConfirmation(String arn, String token) {
//    ConfirmSubscriptionResult result = Receiver.getSNSClient().confirmSubscription(arn, token);
    
//    System.out.println("Confirmed Subsription to [" + arn + "] : " + result.toString());
  }

}
