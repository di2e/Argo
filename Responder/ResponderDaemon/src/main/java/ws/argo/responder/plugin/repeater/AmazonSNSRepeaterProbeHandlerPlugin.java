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

package ws.argo.responder.plugin.repeater;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ws.argo.plugin.probehandler.ProbeHandlerConfigException;
import ws.argo.plugin.probehandler.ProbeHandlerPlugin;
import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.sender.Transport;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSender;
import ws.argo.probe.ProbeSenderException;
import ws.argo.transport.probe.standard.AmazonSNSTransport;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.response.ResponseWrapper;

/**
 * 
 * @author jmsimpson
 *
 */
public class AmazonSNSRepeaterProbeHandlerPlugin implements ProbeHandlerPlugin {

  private static final Logger LOGGER = LogManager.getLogger(AmazonSNSRepeaterProbeHandlerPlugin.class.getName());

  private ProbeSender         _sender;

  @Override
  public ResponseWrapper handleProbeEvent(ProbeWrapper probeWrapper) {
    LOGGER.debug("AmazonSNS Repeater ProbeHandlerPlugin handling probe: " + probeWrapper.asXML());

    ResponseWrapper response = new ResponseWrapper(probeWrapper.getProbeId());

    Probe probe = new Probe(probeWrapper);
    try {
        _sender.sendProbe(probe);
    } catch (ProbeSenderException e) {
        LOGGER.warn( "Unable to repeat probe to Amazon SNS Transport.", e);
    }

    return response;
  }

  @Override
  public void initializeWithPropertiesFilename(String propertiesFilename) throws ProbeHandlerConfigException {
    Properties properties;
    try {
      properties = processPropertiesFile(propertiesFilename);
      initializeProbeSender(properties);
    } catch (TransportConfigException e) {
      throw new ProbeHandlerConfigException("Error reading config [" + propertiesFilename + "]", e);
    }
  }

  @Override
  public String pluginName() {
    return "Amazon SNS Repeater";
  }

  private Properties processPropertiesFile(String propertiesFilename) throws TransportConfigException {
    Properties prop = new Properties();

    InputStream is = null;
    try {
      if (AmazonSNSRepeaterProbeHandlerPlugin.class.getResource(propertiesFilename) != null) {
        is = AmazonSNSRepeaterProbeHandlerPlugin.class.getResourceAsStream(propertiesFilename);
        LOGGER.info("Reading properties file [" + propertiesFilename + "] from classpath.");
      } else {
        is = new FileInputStream(propertiesFilename);
        LOGGER.info("Reading properties file [" + propertiesFilename + "] from file system.");
      }
      prop.load(is);
    } catch (FileNotFoundException e) {
      throw new TransportConfigException(e.getLocalizedMessage(), e);
    } catch (IOException e) {
      throw new TransportConfigException(e.getLocalizedMessage(), e);
    } finally {
      try {
        is.close();
      } catch (Exception e) {
        throw new TransportConfigException(e.getLocalizedMessage(), e);
      }
    }

    return prop;
  }

  private void initializeProbeSender(Properties properties) throws TransportConfigException {
    Transport transport = new AmazonSNSTransport();
    transport.initialize(properties, "");
    _sender = new ProbeSender(transport);
  }

}
