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

package ws.argo.wireline.response;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import ws.argo.wireline.response.xml.ObjectFactory;
import ws.argo.wireline.response.xml.Services;
import ws.argo.wireline.response.xml.Services.Service;

public class ResponseWrapper {
  String                           probeID;
  String                           responseID;
  private HashSet<ServiceWrapper> responses = new HashSet<ServiceWrapper>();

  /**
   * Create a new Response payload for a particular probe.
   * 
   * @param probeID - the ID of the probe as provided by the client
   */
  public ResponseWrapper(String probeID) {
    this.probeID = probeID;
    UUID uuid = UUID.randomUUID();
    this.responseID = "urn:uuid:" + uuid.toString();
  }

  public boolean isEmpty() {
    return responses.isEmpty();
  }

  public int numberOfServices() {
    return responses.size();
  }

  public String getProbeID() {
    return probeID;
  }

  public String getResponseID() {
    return responseID;
  }

  public void addResponse(ServiceWrapper entry) {
    responses.add(entry);
  }

  public HashSet<ServiceWrapper> getServices() {
    return responses;
  }
  
//  /**
//   * Return the XML string form of the Response payload.
//   */
//  public String toXML() {
//    ObjectFactory of = new ObjectFactory();
//    Services xmlServices = of.createServices();
//
//    xmlServices.setProbeID(getProbeID());
//    xmlServices.setResponseID(getResponseID());
//    
//    List<Service> serviceList = xmlServices.getService();
//
//    for (ServiceWrapper infoBean : responses) {
//      serviceList.add(infoBean.xmlService);
//    }
//
//    StringWriter sw = new StringWriter();
//    try {
//      JAXBContext jaxbContext = JAXBContext.newInstance(Services.class);
//      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
//
//      // output pretty printed
//      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//
//      jaxbMarshaller.marshal(xmlServices, sw);
//    } catch (PropertyException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } catch (JAXBException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//    return sw.toString();
//  }
//
//  /**
//   * Return the JSON string form of the Response payload.
//   */
//  public String toJSON() {
//    JSONObject response = this.toJSONObject();
//
//    return response.toString(4);
//  }
//
//  /**
//   * Create the JSON Object for the payload.
//   */
//  public JSONObject toJSONObject() {
//    JSONObject json = new JSONObject();
//    JSONArray array = new JSONArray();
//
//    for (ServiceWrapper infoBean : responses) {
//      array.add(infoBean.asJSONObject());
//    }
//
//    json.put("responseID", this.responseID);
//    json.put("probeID", this.probeID);
//
//    json.put("services", array);
//
//    return json;
//
//  }

}
