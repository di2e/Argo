package ws.argo.probe.transport;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGeneratorException;

/**
 * The AmazonSNSTransport class encapsulates the mechanics of sending the probe
 * via SNS.
 * 
 * @author jmsimpson
 *
 */
public class AmazonSNSTransport implements Transport {

  static final String DEFAULT_TOPIC_NAME = "arn:aws:sns:us-east-1:627164602268:argoDiscoveryProtocol";

  private static final Logger LOGGER = Logger.getLogger(AmazonSNSTransport.class.getName());

  private AmazonSNSClient snsClient;
  private String          argoTopicName = DEFAULT_TOPIC_NAME;

  public AmazonSNSTransport(String ak, String sk) {
    AWSCredentials creds = new BasicAWSCredentials(ak, sk);
    snsClient = new AmazonSNSClient(creds);
  }

  public AmazonSNSTransport(String ak, String sk, String argoTopicName) {
    this(ak, sk);
    this.argoTopicName = argoTopicName;
  }

  @Override
  public void sendProbe(Probe probe) throws ProbeGeneratorException {
    String msg;
    try {
      msg = probe.asXML();
    } catch (JAXBException e) {
      throw new ProbeGeneratorException("Error trying to send probe payload", e);
    }
    PublishRequest publishRequest = new PublishRequest(argoTopicName, msg);
    PublishResult publishResult = snsClient.publish(publishRequest);
    // print MessageId of message published to SNS topic
    LOGGER.log(Level.FINEST, "Send probe payload as message id [" + publishResult.getMessageId() + "]: " + msg);

  }

  @Override
  public int maxPayloadSize() {
    // TODO Auto-generated method stub
    return Integer.MAX_VALUE;
  }

  @Override
  public void close() throws ProbeGeneratorException {
    // Nothing to do
  }

  /**
   * Return the description of this Transport.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("AmazonSNS Transport - ");
    buf.append(" arn [").append(argoTopicName).append("] - client [")
        .append(snsClient.toString()).append("]");

    return buf.toString();
  }

}
