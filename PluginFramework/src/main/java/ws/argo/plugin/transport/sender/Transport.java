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

package ws.argo.plugin.transport.sender;

import java.util.Properties;

import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSenderException;

/**
 * The Transport interface defines the API for sending the wireline payloads.
 * 
 * @author jmsimpson
 *
 */
public interface Transport {

  /**
   * Initialize the transport with the values provided in the Properties object.
   * 
   * @param p the Properties object with the initialization values
   * @param networkInterface name of the network interface
   * @throws TransportConfigException if something goes wrong
   */
  public void initialize(Properties p, String networkInterface) throws TransportConfigException;

  /**
   * Actually send the probe out on transport mechanism.
   * 
   * @param probe the Probe instance that has been pre-configured
   * @throws ProbeSenderException if something bad happened when sending the
   *           probe
   */
  public void sendProbe(Probe probe) throws TransportException;

  /**
   * Return the maximum payload size that this transport can handle. For
   * example, the payload of the UDP Multicast transport could only be 600
   * bytes, meaning that the probe might be split up into several smaller
   * probes. But other transports such as JMS or SNS might allow probe payload
   * sizes much larger (practically unlimited).
   * 
   * @return max payload size in bytes
   */
  public int maxPayloadSize();

  /**
   * Return the name of the network interface associated with this transport.
   * 
   * @return the name of the network interface associated with this transport
   */
  public String getNetworkInterfaceName();

  /**
   * Close the transport.
   * 
   * @throws ProbeSenderException if something bad happened
   */
  public void close() throws TransportException;

}
