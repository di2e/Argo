package ws.argo.plugin.transport.responder;

import java.util.Properties;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSenderException;

/**
 * The Transport interface defines the API for sending the wireline payloads.
 * 
 * @author jmsimpson
 *
 */
public interface Transport_x {

  /**
   * Initialize the transport with the values provided in the Properties object.
   * 
   * @param p the Properties object with the initialization values
   * @param networkInterface name of the network inteface
   * @throws TransportConfigException_x if something goes wrong
   */
  public void initialize(Properties p, String networkInterface) throws TransportConfigException_x;

  /**
   * Actually send the probe out on transport mechanism.
   * 
   * @param probe the Probe instance that has been pre-configured
   * @throws ProbeSenderException if something bad happened when sending the
   *           probe
   */
  public void sendProbe(Probe probe) throws ProbeSenderException;

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

  public String getNetworkInterfaceName();

  /**
   * Close the transport.
   * 
   * @throws ProbeSenderException if something bad happened
   */
  public void close() throws ProbeSenderException;

}
