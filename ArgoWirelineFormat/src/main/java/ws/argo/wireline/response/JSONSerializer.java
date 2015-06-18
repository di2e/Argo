package ws.argo.wireline.response;

import java.util.ArrayList;

import ws.argo.wireline.response.ServiceWrapper.AccessPoint;
import ws.argo.wireline.response.json.Response;
import ws.argo.wireline.response.json.Service;
import ws.argo.wireline.response.json.Service.Consumability;

import com.google.gson.Gson;

public class JSONSerializer {

  public JSONSerializer() {
  }

  public String marshal(ResponseWrapper response) {
    Gson gson = new Gson();

    Response jsonResponse = composeResponseFromResponseWrapper(response);

    String jsonString = gson.toJson(jsonResponse);

    return jsonString;
  }

  private Response composeResponseFromResponseWrapper(ResponseWrapper response) {

    Response jsonResponse = new Response();

    jsonResponse.withProbeID(response.getProbeID()).withResponseID(response.getResponseID());

    ArrayList<Service> jsonServices = new ArrayList<Service>();

    for (ServiceWrapper service : response.getServices()) {
      Service jsonService = composeServiceFromServiceWrapper(service);
      jsonServices.add(jsonService);
    }

    jsonResponse.setServices(jsonServices);

    return jsonResponse;

  }

  private Service composeServiceFromServiceWrapper(ServiceWrapper service) {
    Service jsonService = new Service();

    Consumability consumability = Consumability.fromValue(service.getConsumability());

    jsonService.withId(service.getId()).withServiceName(service.getServiceName()).withConsumability(consumability);
    jsonService.withDescription(service.getDescription()).withContractDescription(service.getContractDescription());
    jsonService.withContractId(service.getServiceContractID());
    if (service.getTtl() != null)
      jsonService.withTtl(service.getTtl().toString());

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

  public ResponseWrapper unmarshal(String payload) throws ResponseParseException {

    return unmarshalJSON(payload);

  }

  private ResponseWrapper unmarshalJSON(String payload) {
    Gson gson = new Gson();

    Response jsonResponse = gson.fromJson(payload, Response.class);

    ResponseWrapper response = new ResponseWrapper(jsonResponse.getProbeID());
    response.setResponseID(jsonResponse.getResponseID());
    
    for (Service jsonService : jsonResponse.getServices()) {
      ServiceWrapper service = new ServiceWrapper(jsonService.getId());
      if (jsonService.getConsumability() != null)
        service.setConsumability(jsonService.getConsumability().name());
      service.setDescription(jsonService.getDescription());
      service.setContractDescription(jsonService.getContractDescription());
      service.setServiceContractID(jsonService.getContractId());
      service.setServiceName(jsonService.getServiceName());
      service.setTtl(jsonService.getTtl());
      
      for (ws.argo.wireline.response.json.AccessPoint ap : jsonService.getAccessPoints()) {
        service.addAccessPoint(ap.getLabel(), ap.getIpAddress(), ap.getPort(), ap.getUrl(), ap.getDataType(), ap.getData());
      }
      
      response.addResponse(service);
      
    }

    return response;

  }

}
