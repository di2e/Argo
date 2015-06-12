package ws.argo.wireline.response;

import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;

import ws.argo.wireline.response.ServiceWrapper.AccessPoint;
import ws.argo.wireline.response.json.Response;
import ws.argo.wireline.response.json.Service;
import ws.argo.wireline.response.json.Service.Consumability;
import ws.argo.wireline.response.xml.Services.Service.AccessPoints;
import ws.argo.wireline.response.xml.Services.Service.AccessPoints.AccessPoint.Data;

import com.google.gson.Gson;

public class JSONSerializer {

  private ResponseWrapper response;

  public JSONSerializer(ResponseWrapper response) {
    this.response = response;
  }

  public String serialize() {
    Gson gson = new Gson(); 
    
    Response jsonResponse = composeResponseFromResponseWrapper(this.response);
    
    String jsonString = gson.toJson(jsonResponse);
    
    return jsonString;
  }
  
  private Response composeResponseFromResponseWrapper(ResponseWrapper response) {
    
    Response jsonResponse = new Response();
    
    jsonResponse.withProbeID(response.getProbeID()).withResponseID(response.getResponseID());
    
    ArrayList<Service> jsonServices = new ArrayList<Service>();
    
    for (ServiceWrapper service : response.getServices() ) {
      Service jsonService = composeServiceFromServiceWrapper(service);
      jsonServices.add(jsonService);
    }
    
    return jsonResponse;
    
  }
  
  private Service composeServiceFromServiceWrapper(ServiceWrapper service) {
    Service jsonService = new Service();
    
    Consumability consumability = Consumability.fromValue(service.getConsumability());
    
    jsonService.withId(service.getId()).withServiceName(service.getServiceName()).withConsumability(consumability);
    jsonService.withDescription(service.getDescription()).withContractDescription(service.getContractDescription());
    jsonService.withTtl(service.getTtl().toString()).withContractId(service.getServiceContractID());
 
    ArrayList<ws.argo.wireline.response.json.AccessPoint> accessPoints = new ArrayList<ws.argo.wireline.response.json.AccessPoint>();
    
    for (AccessPoint ap : service.accessPoints) {
      
      ws.argo.wireline.response.json.AccessPoint jsonap = new ws.argo.wireline.response.json.AccessPoint();
      
      jsonap.withLabel(ap.label).withIpAddress(ap.ipAddress).withPort(ap.port).withUrl(ap.port);
      jsonap.withDataType(ap.dataType).withData(ap.data);
      
      accessPoints.add(jsonap);

    }
    
    jsonService.setAccessPoints(accessPoints);

    return jsonService;
    
  }
  
  /**
   * Return the JSON String form of the Service Record.
   */
  /*
  public JSONObject asJSONObject() {
    JSONObject json = new JSONObject();

    json.put("id", this.getId());
    json.put("serviceContractID", this.getServiceContractID());
    json.put("serviceName", this.getServiceName());
    json.put("description", this.getDescription());
    json.put("contractDescription", this.getContractDescription());
    json.put("consumability", this.getConsumability());
    json.put("ttl", this.getTtl().toString());

    JSONArray array = new JSONArray();

    for (AccessPoint ap : xmlService.getAccessPoints().getAccessPoint()) {
      JSONObject jsonAP = new JSONObject();

      jsonAP.put("label", ap.getLabel());
      jsonAP.put("ipAddress", ap.getIpAddress());
      jsonAP.put("port", ap.getPort());
      jsonAP.put("url", ap.getUrl());

      if (ap.getData() != null) {
        Data xmlData = ap.getData();
        jsonAP.put("dataType", xmlData.getType());
        byte[] bytesEncoded = Base64.encodeBase64(xmlData.getValue()
            .getBytes());
        String encodedString = new String(bytesEncoded);
        jsonAP.put("data", encodedString);
      } else {
        jsonAP.put("dataType", "");
        jsonAP.put("data", "");
      }

      array.add(jsonAP);
    }

    json.put("accessPoints", array);

    return json;
  }
  */
  
//  /**
//   * Return the JSON String form of the Service Record.
//   */
//  public JsonObject asJSON() {
//    Gson gson = new Gson();
    
    
    
//    JSONObject json = new JSONObject();
//
//    json.put("id", this.getId());
//    json.put("serviceContractID", this.getServiceContractID());
//    json.put("serviceName", this.getServiceName());
//    json.put("description", this.getDescription());
//    json.put("contractDescription", this.getContractDescription());
//    json.put("consumability", this.getConsumability());
//    json.put("ttl", this.getTtl().toString());
//
//    JSONArray array = new JSONArray();
//
//    for (AccessPoint ap : xmlService.getAccessPoints().getAccessPoint()) {
//      JSONObject jsonAP = new JSONObject();
//
//      jsonAP.put("label", ap.getLabel());
//      jsonAP.put("ipAddress", ap.getIpAddress());
//      jsonAP.put("port", ap.getPort());
//      jsonAP.put("url", ap.getUrl());
//
//      if (ap.getData() != null) {
//        Data xmlData = ap.getData();
//        jsonAP.put("dataType", xmlData.getType());
//        byte[] bytesEncoded = Base64.encodeBase64(xmlData.getValue()
//            .getBytes());
//        String encodedString = new String(bytesEncoded);
//        jsonAP.put("data", encodedString);
//      } else {
//        jsonAP.put("dataType", "");
//        jsonAP.put("data", "");
//      }
//
//      array.add(jsonAP);
//    }
//
//    json.put("accessPoints", array);
//
//    return json;
//  }


}
