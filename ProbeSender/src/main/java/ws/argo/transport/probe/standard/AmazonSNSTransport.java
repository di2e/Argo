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

package ws.argo.transport.probe.standard;

import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;
import ws.argo.plugin.transport.sender.Transport;
import ws.argo.probe.Probe;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

/**
 * The AmazonSNSTransport class encapsulates the mechanics of sending the probe
 * via SNS.
 * 
 * @author jmsimpson
 *
 */
public class AmazonSNSTransport implements Transport {

  // static final String DEFAULT_TOPIC_NAME =
  // "arn:aws:sns:us-east-1:627164602268:argoDiscoveryProtocol";

  private static final Logger LOGGER = LogManager.getLogger(AmazonSNSTransport.class.getName());

  private AmazonSNSClient     snsClient;
  private String              argoTopicName;
  private String              ak;
  private String              sk;

  /**
   * Default constructor. Usually followed by the
   * {@linkplain #initialize(Properties, String)} call.
   * 
   */
  public AmazonSNSTransport() {
  }

  public AmazonSNSTransport(String ak, String sk) {
    AWSCredentials creds = new BasicAWSCredentials(ak, sk);
    snsClient = new AmazonSNSClient(creds);
  }

  public AmazonSNSTransport(String ak, String sk, String argoTopicName) {
    this(ak, sk);
    this.argoTopicName = argoTopicName;
  }

  @Override
  public void initialize(Properties p, String networkInterface) throws TransportConfigException {
    this.argoTopicName = p.getProperty("argoTopicName");
    this.ak = p.getProperty("amazonAccessKey");
    this.sk = p.getProperty("amazonSecretKey");

    // Gotta have all three configuration items or KABOOM.
    // Can't really have a default for any of these.
    if (this.argoTopicName == null || this.ak == null || this.sk == null)
      throw new TransportConfigException("The Topic Name, AK and/or the SK was not specified.");

    AWSCredentials creds = new BasicAWSCredentials(ak, sk);
    snsClient = new AmazonSNSClient(creds);
  }

  @Override
  public void sendProbe(Probe probe) throws TransportException {
    String msg;
    try {
      msg = probe.asXML();
    } catch (JAXBException e) {
      throw new TransportException("Error trying to send probe payload", e);
    }
    PublishRequest publishRequest = new PublishRequest(argoTopicName, msg);
    PublishResult publishResult = snsClient.publish(publishRequest);
    // print MessageId of message published to SNS topic
    LOGGER.debug( "Send probe payload as message id [" + publishResult.getMessageId() + "]: " + msg);

  }

  @Override
  public int maxPayloadSize() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void close() throws TransportException {
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

  @Override
  public String getNetworkInterfaceName() {
    return "UNASSIGNED";
  }

}
