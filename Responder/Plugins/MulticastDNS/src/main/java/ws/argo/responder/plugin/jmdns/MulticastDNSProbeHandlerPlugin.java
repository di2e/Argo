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

package ws.argo.responder.plugin.jmdns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import ws.argo.plugin.probehandler.ProbeHandlerConfigException;
import ws.argo.plugin.probehandler.ProbeHandlerPlugin;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.response.ResponseWrapper;
import ws.argo.wireline.response.ServiceWrapper;

public class MulticastDNSProbeHandlerPlugin implements ServiceListener, ServiceTypeListener,
    ProbeHandlerPlugin {

  private static final Logger LOGGER      = Logger.getLogger(MulticastDNSProbeHandlerPlugin.class.getName());

  protected JmDNS             jmmDNS;
  ArrayList<ServiceWrapper>   serviceList = new ArrayList<ServiceWrapper>();

  public MulticastDNSProbeHandlerPlugin() {
  }

  @Override
  public ResponseWrapper handleProbeEvent(ProbeWrapper probe) {
    // TODO Auto-generated method stub
    ResponseWrapper response = new ResponseWrapper(probe.getProbeId());

    LOGGER.fine("Handling probe: " + probe.asXML());

    // do the actual lookup here
    // and create and return the ResponderPayload
    // Can you say O(n^2) lookup? Very bad - we can fix later

    if (probe.isNaked()) {
      LOGGER.fine("Query all detected - no service contract IDs in probe");
      for (ServiceWrapper entry : serviceList) {
        // If the set of contract IDs is empty, get all of them
        response.addResponse(entry);
      }

    } else {
      for (String serviceContractID : probe.getServiceContractIDs()) {
        LOGGER.fine("Looking to detect " + serviceContractID + " in entry list.");
        for (ServiceWrapper entry : serviceList) {
          if (entry.getServiceContractID().equals(serviceContractID)) {
            // Boom Baby - we got one!!!
            response.addResponse(entry);
          }
        }
        // Do "instance" name lookup here
      }
    }

    return response;
  }

  @Override
  public void initializeWithPropertiesFilename(String filename) throws ProbeHandlerConfigException {
    LOGGER.info("Does not support loading props");
    try {
      initialize();
    } catch (IOException e) {
      throw new ProbeHandlerConfigException("Error initializing handler.", e);
    }
  }

  private void initialize() throws IOException {
    this.jmmDNS = JmDNS.create(); // JmmDNS.Factory.getInstance();
    try {
      this.jmmDNS.addServiceTypeListener(this);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void serviceTypeAdded(ServiceEvent event) {
    // TODO Auto-generated method stub
    LOGGER.info("mDNS Service Type Added: " + event.getType());
    this.jmmDNS.addServiceListener(event.getType(), this);

  }

  @Override
  public void subTypeForServiceTypeAdded(ServiceEvent event) {
    // TODO Auto-generated method stub
    LOGGER.info("mDNS Service Sub-Type Added: " + event.getType());

  }

  @Override
  public void serviceAdded(ServiceEvent event) {
    // TODO Auto-generated method stub
    LOGGER.info("mDNS Service Added: " + event.getName());

  }

  @Override
  public void serviceRemoved(ServiceEvent event) {
    // TODO Auto-generated method stub
    LOGGER.info("mDNS Service Removed: " + event.getName());

  }

  @Override
  public void serviceResolved(ServiceEvent event) {
    LOGGER.info("mDNS Service RESOLVED: " + event.toString());

    if (!event.getInfo().hasData()) {
      LOGGER.warning("mDNS Service has no data - skipping: " + event.toString());
      return;
    }

    String contractID = null;
    String serviceID = null;

    contractID = event.getInfo().getType();
    serviceID = event.getInfo().getQualifiedName();

    ServiceWrapper config = new ServiceWrapper(event.getInfo().getKey());

    config.setServiceContractID(contractID);
    config.setServiceName(event.getInfo().getName());

    java.net.Inet4Address[] ipv4Addresses = event.getInfo().getInet4Addresses();
    for (java.net.Inet4Address addr : ipv4Addresses) {
      config.addAccessPoint("IPv4", addr.getHostAddress(), Integer.toString(event.getInfo().getPort()), "", "text", event.getInfo().getNiceTextString());
    }

    java.net.Inet6Address[] ipv6Addresses = event.getInfo().getInet6Addresses();
    for (java.net.Inet6Address addr : ipv6Addresses) {
      config.addAccessPoint("IPv6", addr.getHostAddress(), Integer.toString(event.getInfo().getPort()), "", "text", event.getInfo().getNiceTextString());
    }

    for (String url : event.getInfo().getURLs()) {
      config.addAccessPoint("URL", "", "", url, "text", event.getInfo().getNiceTextString());
    }

    config.setDescription(event.getInfo().getQualifiedName());

    // String tempData = new String(config.data);
    //
    // List<String> allMatches = new ArrayList<String>();
    // Matcher m = Pattern.compile("(\\\\[0-9]{3})").matcher(tempData);
    // while (m.find()) {
    // allMatches.add(m.group());
    // }
    // for (String octal: allMatches) {
    // int charNum = Integer.parseInt(octal.substring(1));
    // String replacementChar = Character.toString ((char) charNum);
    // tempData = tempData.replace(octal, replacementChar);
    // }
    //
    // config.data = tempData;

    config.setContractDescription(event.getInfo().getApplication());

    config.setConsumability(ServiceWrapper.MACHINE_CONSUMABLE);

    config.setTtl(0);

    serviceList.add(config);

  }

}
