package ws.argo.probe.transport;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGeneratorException;

/**
 * The Transport interface defines the API for sending the wireline 
 * @author jmsimpson
 *
 */
public interface Transport {

  /**
   * Actually send the probe out on transport mechanism.
   * 
   * @param probe the Probe instance that has been pre-configured
   * @throws ProbeGeneratorException if something bad happened when sending the
   *           probe
   */
  public void sendProbe(Probe probe) throws ProbeGeneratorException;
  
  /**
   * Return the maximum payload size that this transport can handle.
   * For example, the payload of the UDP Multicast transport could only
   * be 600 bytes, meaning that the probe might be split up into
   * several smaller probes.  But other transports such as JMS or SNS
   * might allow probe payload sizes much larger (practically unlimited).
   * 
   * @return max payload size in bytes
   */
  public int maxPayloadSize();

  /**
   * Close the transport.
   */
  public void close() throws ProbeGeneratorException;
  
}
