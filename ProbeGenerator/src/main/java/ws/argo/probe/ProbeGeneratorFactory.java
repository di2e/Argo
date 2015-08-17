package ws.argo.probe;

import ws.argo.probe.transport.AmazonSNSTransport;
import ws.argo.probe.transport.MulticastTransport;
import ws.argo.probe.transport.Transport;

/**
 * The ProbeGeneratorFactor is a convenience class that offers static methods
 * for generating various sorts of ProbeGenerators with different Transports.
 * You could certainly create all of these by hand if you like, but we thought
 * we'd be nice. If you have your own Transport type, then you'll have to do
 * work like this yourself.
 * 
 * @author jmsimpson
 *
 */
public class ProbeGeneratorFactory {

  // Factory methods

  /**
   * Create a Multicast ProbeGenerator with all the default values. The default
   * values are the default multicast group address and port of the Argo
   * protocol. The Network Interface is the NI associated with the localhost
   * address.
   * 
   * @return configured ProbeGenerator instance
   * @throws ProbeGeneratorException if something went wrong
   */
  public static ProbeGenerator createMulticastProbeGenerator() throws ProbeGeneratorException {
    Transport mcastTransport = new MulticastTransport();
    ProbeGenerator gen = new ProbeGenerator(mcastTransport);
    return gen;
  }

  /**
   * Create a Multicast ProbeGenerator specifying the Network Interface to send
   * on.
   * 
   * @return configured ProbeGenerator instance
   * @throws ProbeGeneratorException if something went wrong
   */
  public static ProbeGenerator createMulticastProbeGenerator(String niName) throws ProbeGeneratorException {
    Transport mcastTransport = new MulticastTransport(niName);
    ProbeGenerator gen = new ProbeGenerator(mcastTransport);
    return gen;
  }

  /**
   * Create a Multicast ProbeGenerator specifying the multicast group and port
   * to send on.
   * 
   * @return configured ProbeGenerator instance
   * @throws ProbeGeneratorException if something went wrong
   */
  public static ProbeGenerator createMulticastProbeGenerator(String mcastGroup, int port) throws ProbeGeneratorException {
    Transport mcastTransport = new MulticastTransport(mcastGroup, port);
    ProbeGenerator gen = new ProbeGenerator(mcastTransport);
    return gen;
  }

  /**
   * Create a Multicast ProbeGenerator specifying the multicast group and port
   * and the Network Interface to send on.
   * 
   * @return configured ProbeGenerator instance
   * @throws ProbeGeneratorException if something went wrong
   */
  public static ProbeGenerator createMulticastProbeGenerator(String mcastGroup, int port, String niName) throws ProbeGeneratorException {
    Transport mcastTransport = new MulticastTransport(mcastGroup, port, niName);
    ProbeGenerator gen = new ProbeGenerator(mcastTransport);
    return gen;
  }

  /**
   * Create a AmazonSNS transport ProbeGenerator using the default values.
   * 
   * @return configured ProbeGenerator instance
   * @throws ProbeGeneratorException if something went wrong
   */
  public static ProbeGenerator createSNSProbeGenerator(String ak, String sk) throws ProbeGeneratorException {
    Transport snsTransport = new AmazonSNSTransport(ak, sk);
    ProbeGenerator gen = new ProbeGenerator(snsTransport);
    return gen;
  }

  /**
   * Create a AmazonSNS transport ProbeGenerator specifying the Argo SNS topic
   * name.
   * 
   * @return configured ProbeGenerator instance
   * @throws ProbeGeneratorException if something went wrong
   */
  public static ProbeGenerator createSNSProbeGenerator(String ak, String sk, String argoTopicName) throws ProbeGeneratorException {
    Transport snsTransport = new AmazonSNSTransport(ak, sk ,argoTopicName);
    ProbeGenerator gen = new ProbeGenerator(snsTransport);
    return gen;
  }
  
}
