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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.wireline.response.JSONSerializer;
import ws.argo.wireline.response.ResponseParseException;
import ws.argo.wireline.response.ResponseWrapper;
import ws.argo.wireline.response.ServiceWrapper;
import ws.argo.wireline.response.XMLSerializer;

/**
 * This is the CL UI probe listener resource used by the JAX-RS container. It
 * provides the REST API for the "respondTo" URL used in the probes.
 * 
 * @author jmsimpson
 *
 */
@Path("/listener")
public class ProbeResponseResource {

  private static ResponseCache cache = new ResponseCache();

  private static void resetCache() {
    cache = new ResponseCache();
  }

  /**
   * Inbound JSON responses get processed here.
   * 
   * @param probeResponseJSON - the actual wireline response payload
   * @return some innocuous string
   */
  @POST
  @Path("/probeResponse")
  @Consumes("application/json")
  public String handleJSONProbeResponse(String probeResponseJSON) {

    JSONSerializer serializer = new JSONSerializer();

    ResponseWrapper response;
    try {
      response = serializer.unmarshal(probeResponseJSON);
    } catch (ResponseParseException e) {
      String errorResponseString = "Incoming Response could not be parsed. Error message is: " + e.getMessage();
      Console.error(errorResponseString);
      Console.error("Wireline message that could no be parsed is:");
      Console.error(probeResponseJSON);
      return errorResponseString;
    }

    for (ServiceWrapper service : response.getServices()) {
      service.setResponseID(response.getResponseID());
      service.setProbeID(response.getProbeID());
      cache.cache(new ExpiringService(service));
    }

    String statusString = "\nSuccessfully cached " + response.getServices().size() + " services";
    Console.info(statusString);

    return statusString;
  }

  /**
   * Inbound XML responses get processed here.
   * 
   * @param probeResponseXML - the actual wireline response payload
   * @return some innocuous string
   */
  @POST
  @Path("/probeResponse")
  @Consumes("application/xml")
  public String handleXMLProbeResponse(String probeResponseXML) {

    XMLSerializer serializer = new XMLSerializer();

    ResponseWrapper response;
    try {
      response = serializer.unmarshal(probeResponseXML);
    } catch (ResponseParseException e) {
      String errorResponseString = "Incoming Response could not be parsed. Error message is: " + e.getMessage();
      Console.error(errorResponseString);
      Console.error("Wireline message that could no be parsed is:");
      Console.error(probeResponseXML);
      return errorResponseString;
    }

    for (ServiceWrapper service : response.getServices()) {
      service.setResponseID(response.getResponseID());
      service.setProbeID(response.getProbeID());
      cache.cache(new ExpiringService(service));
    }

    String statusString = "\nSuccessfully cached " + response.getServices().size() + " services";
    Console.info(statusString);

    return statusString;
  }

  @GET
  @Path("/responses")
  @Produces("application/json")
  public String getResponses() {
    return cache.asJSON();
  }

  @GET
  @Path("/clearCache")
  @Produces("application/json")
  public String clearCache() {
    resetCache();
    return "Cleared Cache";
  }

}
