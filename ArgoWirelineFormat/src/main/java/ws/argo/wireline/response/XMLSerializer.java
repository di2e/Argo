package ws.argo.wireline.response;

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import ws.argo.wireline.response.ServiceWrapper.AccessPoint;
import ws.argo.wireline.response.xml.ObjectFactory;
import ws.argo.wireline.response.xml.Services;
import ws.argo.wireline.response.xml.Services.Service;
import ws.argo.wireline.response.xml.Services.Service.AccessPoints;
import ws.argo.wireline.response.xml.Services.Service.AccessPoints.AccessPoint.Data;

public class XMLSerializer {

  private ResponseWrapper response;

  public XMLSerializer(ResponseWrapper response) {
    this.response = response;
  }

  public String serialize() {

    Services xmlServices = this.composeResponseFromResponseWrapper(response);
    
    StringWriter sw = new StringWriter();
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Services.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

      // output pretty printed
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

      jaxbMarshaller.marshal(xmlServices, sw);
    } catch (PropertyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return sw.toString();
  }

  private Services composeResponseFromResponseWrapper(ResponseWrapper response) {

    ObjectFactory of = new ObjectFactory();
    Services xmlServices = of.createServices();

    xmlServices.setProbeID(response.getProbeID());
    xmlServices.setResponseID(response.getResponseID());

    List<Service> serviceList = xmlServices.getService();

    for (ServiceWrapper service : response.getServices()) {
      Service xmlService = this.composeServiceFromServiceWrapper(service);
      serviceList.add(xmlService);
    }

    return xmlServices;

  }

  private Service composeServiceFromServiceWrapper(ServiceWrapper service) {
    ObjectFactory of = new ObjectFactory();
    Service xmlService = of.createServicesService();

    xmlService.setId(service.getId());
    xmlService.setServiceName(service.getServiceName());
    xmlService.setTtl(Integer.toString(service.getTtl().intValue()));
    xmlService.setConsumability(service.getConsumability());
    xmlService.setContractDescription(service.getContractDescription());
    xmlService.setDescription(service.getDescription());
    xmlService.setContractID(service.getServiceContractID());

    for (AccessPoint ac : service.accessPoints) {
      ws.argo.wireline.response.xml.Services.Service.AccessPoints.AccessPoint ap = of.createServicesServiceAccessPointsAccessPoint();
      ap.setLabel(ac.label);
      ap.setIpAddress(ac.ipAddress);
      ap.setPort(ac.port);
      ap.setUrl(ac.url);
      Data xmlData = of.createServicesServiceAccessPointsAccessPointData();
      xmlData.setType(ac.dataType);
      xmlData.setValue(ac.data);
      ap.setData(xmlData);

      AccessPoints aps = xmlService.getAccessPoints();
      if (aps == null) {
        aps = of.createServicesServiceAccessPoints();
        xmlService.setAccessPoints(aps);
      }

      xmlService.getAccessPoints().getAccessPoint().add(ap);

    }
    return xmlService;

  }

}
