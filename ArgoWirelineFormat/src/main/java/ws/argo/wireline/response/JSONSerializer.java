package ws.argo.wireline.response;

import java.util.ArrayList;

import ws.argo.wireline.response.ServiceWrapper.AccessPoint;
import ws.argo.wireline.response.json.Response;
import ws.argo.wireline.response.json.Service;
import ws.argo.wireline.response.json.Service.Consumability;

import com.google.gson.Gson;

/**
 * /** The JSONSerializer provides the translation between the ResponseWrapper
 * and the wireline strings. It can both marshall and unmarshall wireline
 * protocol from the domain objects.
 * 
 * @author jmsimpson
 *
 */
public class JSONSerializer {

  public JSONSerializer() {
  }

  /**
   * Translate the ResponseWrapper into the wireline string. See {@link ResponseWrapper}
   * 
   * @param response the instance of the ResponseWrapper
   * @return the wireline string
   */
  public String marshal(ResponseWrapper response) {
    Gson gson = new Gson();

    Response jsonResponse = composeResponseFromResponseWrapper(response);

    String jsonString = gson.toJson(jsonResponse);

    return jsonString;
  }

  /**
   * Translate the ServiceWrapper into the wireline string.  See {@link ServiceWrapper}
   * 
   * @param service the instance of the ServiceWrapper
   * @return the wireline string
   */
  public String marshalService(ServiceWrapper service) {
    Gson gson = new Gson();

    Service jsonService = composeServiceFromServiceWrapper(service);

    String jsonString = gson.toJson(jsonService);

    return jsonString;
  }

  private Response composeResponseFromResponseWrapper(ResponseWrapper response) {

    Response jsonResponse = new Response();

    jsonResponse.withProbeID(response.getProbeID()).withResponseID(response.getResponseID());

    ArrayList<Service> jsonServices = new ArrayList<Service>();

    for (ServiceWrapper service : response.getServices()) {
      service.setResponseID(response.getResponseID());
      service.setProbeID(response.getProbeID());
      
      Service jsonService = composeServiceFromServiceWrapper(service);
      jsonServices.add(jsonService);
    }

    jsonResponse.setServices(jsonServices);

    return jsonResponse;

  }

  private Service composeServiceFromServiceWrapper(ServiceWrapper service) {
    Service jsonService = new Service();

    Consumability consumability = Consumability.fromValue(service.getConsumability());

    jsonService.withProbeId(service.getProbeId()).withProbeId(service.getProbeId());
    
    jsonService.withId(service.getId()).withServiceName(service.getServiceName()).withConsumability(consumability);
    jsonService.withDescription(service.getDescription()).withContractDescription(service.getContractDescription());
    jsonService.withContractId(service.getServiceContractID());
    if (service.getTtl() != null)
      jsonService.withTtl(service.getTtl().toString());

    ArrayList<ws.argo.wireline.response.json.AccessPoint> accessPoints = new ArrayList<ws.argo.wireline.response.json.AccessPoint>();

    for (AccessPoint ap : service.accessPoints) {

      ws.argo.wireline.response.json.AccessPoint jsonap = new ws.argo.wireline.response.json.AccessPoint();

      jsonap.withLabel(ap.label).withIpAddress(ap.ipAddress).withPort(ap.port).withUrl(ap.url);
      jsonap.withDataType(ap.dataType).withData(ap.data);

      accessPoints.add(jsonap);

    }

    jsonService.setAccessPoints(accessPoints);

    return jsonService;

  }

  /**
   * Translate the wireline string into an instance of a ResponseWrapper object.
   * 
   * @param payload the wireline string
   * @return a new instance of a {@link ResponseWrapper}.
   * @throws ResponseParseException if some issues occurred parsing the response
   */
  public ResponseWrapper unmarshal(String payload) throws ResponseParseException {

    return unmarshalResponseJSON(payload);

  }

  /**
   * Translate the wireline string to an instance of a ServiceWrapper object.
   * 
   * @param payload the wireline string
   * @return a new instance of a {@link ServiceWrapper}.
   * @throws ResponseParseException if some issues occurred parsing the response
   */
  public ServiceWrapper unmarshalService(String payload) throws ResponseParseException {

    return unmarshalServiceJSON(payload);

  }

  private ResponseWrapper unmarshalResponseJSON(String payload) {
    Gson gson = new Gson();

    Response jsonResponse = gson.fromJson(payload, Response.class);

    ResponseWrapper response = new ResponseWrapper(jsonResponse.getProbeID());
    response.setResponseID(jsonResponse.getResponseID());

    for (Service jsonService : jsonResponse.getServices()) {
      ServiceWrapper service = createServiceWrapperFromService(jsonService);

      response.addResponse(service);

    }

    return response;

  }

  private ServiceWrapper unmarshalServiceJSON(String payload) {
    Gson gson = new Gson();

    Service jsonService = gson.fromJson(payload, Service.class);

    ServiceWrapper service = createServiceWrapperFromService(jsonService);

    return service;
  }

  private ServiceWrapper createServiceWrapperFromService(Service jsonService) {
    ServiceWrapper service = new ServiceWrapper(jsonService.getId());
    if (jsonService.getConsumability() != null)
      service.setConsumability(jsonService.getConsumability().name());
    service.setDescription(jsonService.getDescription());
    service.setContractDescription(jsonService.getContractDescription());
    service.setServiceContractID(jsonService.getContractId());
    service.setServiceName(jsonService.getServiceName());
    service.setTtl(jsonService.getTtl());
    
    service.setResponseID(jsonService.getResponseId());
    service.setProbeID(jsonService.getProbeId());

    for (ws.argo.wireline.response.json.AccessPoint ap : jsonService.getAccessPoints()) {
      service.addAccessPoint(ap.getLabel(), ap.getIpAddress(), ap.getPort(), ap.getUrl(), ap.getDataType(), ap.getData());
    }
    return service;
  }

}
