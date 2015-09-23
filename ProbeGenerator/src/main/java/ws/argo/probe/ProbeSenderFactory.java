package ws.argo.probe;

import ws.argo.probe.transport.AmazonSNSTransport;
import ws.argo.probe.transport.MulticastTransport;
import ws.argo.probe.transport.Transport;
import ws.argo.probe.transport.TransportConfigException;

/**
 * The ProbeSenderFactor is a convenience class that offers static methods
 * for generating various sorts of ProbeSenders with different Transports.
 * You could certainly create all of these by hand if you like, but we thought
 * we'd be nice. If you have your own Transport type, then you'll have to do
 * work like this yourself.
 * 
 * @author jmsimpson
 *
 */
public class ProbeSenderFactory {

  // Factory methods

  /**
   * Create a Multicast ProbeSender with all the default values. The default
   * values are the default multicast group address and port of the Argo
   * protocol. The Network Interface is the NI associated with the localhost
   * address.
   * 
   * @return configured ProbeSender instance
   * @throws ProbeSenderException if something went wrong
   * @throws TransportConfigException if something went wrong
   */
  public static ProbeSender createMulticastProbeSender() throws ProbeSenderException, TransportConfigException {
    
    Transport mcastTransport = new MulticastTransport("");
    ProbeSender gen = new ProbeSender(mcastTransport);
    return gen;
  }

  /**
   * Create a Multicast ProbeSender specifying the Network Interface to send
   * on.
   * 
   * @return configured ProbeSender instance
   * @throws ProbeSenderException if something went wrong
   */
  public static ProbeSender createMulticastProbeSender(String niName) throws TransportConfigException {
    Transport mcastTransport = new MulticastTransport(niName);
    ProbeSender gen = new ProbeSender(mcastTransport);
    return gen;
  }

  /**
   * Create a Multicast ProbeSender specifying the multicast group and port
   * to send on.
   * 
   * @return configured ProbeSender instance
   * @throws ProbeSenderException if something went wrong
   * @throws TransportConfigException if there was an issue initializing the transport
   */
  public static ProbeSender createMulticastProbeSender(String mcastGroup, int port) throws TransportConfigException {
    Transport mcastTransport = new MulticastTransport(mcastGroup, port);
    ProbeSender gen = new ProbeSender(mcastTransport);
    return gen;
  }

  /**
   * Create a Multicast ProbeSender specifying the multicast group and port
   * and the Network Interface to send on.
   * 
   * @return configured ProbeSender instance
   * @throws TransportConfigException if something went wrong
   */
  public static ProbeSender createMulticastProbeSender(String mcastGroup, int port, String niName) throws  TransportConfigException {
    Transport mcastTransport = new MulticastTransport(mcastGroup, port, niName);
    ProbeSender gen = new ProbeSender(mcastTransport);
    return gen;
  }

  /**
   * Create a AmazonSNS transport ProbeSender using the default values.
   * 
   * @return configured ProbeSender instance
   * @throws ProbeSenderException if something went wrong
   */
  public static ProbeSender createSNSProbeSender(String ak, String sk) throws ProbeSenderException {
    Transport snsTransport = new AmazonSNSTransport(ak, sk);
    ProbeSender gen = new ProbeSender(snsTransport);
    return gen;
  }

  /**
   * Create a AmazonSNS transport ProbeSender specifying the Argo SNS topic
   * name.
   * 
   * @return configured ProbeSender instance
   * @throws ProbeSenderException if something went wrong
   */
  public static ProbeSender createSNSProbeSender(String ak, String sk, String argoTopicName) throws ProbeSenderException {
    Transport snsTransport = new AmazonSNSTransport(ak, sk ,argoTopicName);
    ProbeSender gen = new ProbeSender(snsTransport);
    return gen;
  }
  
}
