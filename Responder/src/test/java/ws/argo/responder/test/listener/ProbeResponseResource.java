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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.xml.sax.SAXException;

import ws.argo.wireline.response.JSONSerializer;
import ws.argo.wireline.response.ResponseParseException;
import ws.argo.wireline.response.ResponseWrapper;
import ws.argo.wireline.response.ServiceWrapper;
import ws.argo.wireline.response.XMLSerializer;
import ws.argo.wireline.response.xml.Services;
import ws.argo.wireline.response.xml.Services.Service;
import ws.argo.wireline.response.xml.Services.Service.AccessPoints.AccessPoint;
import ws.argo.wireline.response.xml.Services.Service.AccessPoints.AccessPoint.Data;

@Path("/listener")
public class ProbeResponseResource {

  private static ResponseCache cache = new ResponseCache();

/**
 * 
 * @param probeResponseJSON
 * @return
 * @throws ResponseParseException
 */
  @POST
  @Path("/probeResponse")
  @Consumes("application/json")
  public String handleJSONProbeResponse(String probeResponseJSON) throws ResponseParseException {
    System.out.println("Listener receiving JSON probe response: " + probeResponseJSON);
    
    JSONSerializer serializer = new JSONSerializer();
    
    ResponseWrapper response = serializer.unmarshal(probeResponseJSON);
    
    for (ServiceWrapper service : response.getServices()) {
      cache.cache(new ExpiringService(service));
    }

    return "Successfully cached " + response.getServices().size() + " services";
  }

  /**
   * 
   * @param probeResponseXML
   * @return
   * @throws ResponseParseException
   */
  @POST
  @Path("/probeResponse")
  @Consumes("application/xml")
  public String handleXMLProbeResponse(String probeResponseXML) throws ResponseParseException {
    System.out.println("Listener receiving XML probe response: " + probeResponseXML);

    XMLSerializer serializer = new XMLSerializer();
    
    ResponseWrapper response = serializer.unmarshal(probeResponseXML);
    
    for (ServiceWrapper service : response.getServices()) {
      cache.cache(new ExpiringService(service));
    }

    return "Successfully cached " + response.getServices().size() + " services";
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
    cache = new ResponseCache();
    return "Cleared Cache";
  }


}
