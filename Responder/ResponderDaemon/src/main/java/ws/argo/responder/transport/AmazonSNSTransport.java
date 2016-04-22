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

package ws.argo.responder.transport;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;

import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.responder.ProbeProcessor;
import ws.argo.plugin.transport.responder.Transport;
import ws.argo.responder.Responder;
import ws.argo.responder.transport.sns.SNSListener;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.SubscribeRequest;

/**
 * The AmazonSNSTransport is a transport that uses the Amazon SNS service as the
 * pub/sub mechanism to move probes around.
 * 
 * @author jmsimpson
 *
 */
public class AmazonSNSTransport implements Transport {

  private static final Logger LOGGER     = LogManager.getLogger(AmazonSNSTransport.class.getName());

  private HttpServer          _server;
  WebTarget                   target;

  private AmazonSNSClient     _snsClient;
  private String              _argoTopicName;
  private boolean             _inShutdown = false;

  // Configuration params
  private String              _subscriptionArn;
  private ProbeProcessor      _processor;
  private String              _listenerURL;
  private String              _amazonAK;
  private String              _amazonSK;
  private String              _subscriptionURL;

  public AmazonSNSTransport() {
  }

  @Override
  public void run() {

    URI uri;

    try {
      uri = getBaseListenerURI();
    } catch (URISyntaxException e) {
      LOGGER.warn("The listenerURL specified in the configuration file [" + _listenerURL + "] is invalid. ");
      LOGGER.info("Using the default listner URL assocaited with the loca host [" + SNSListener.getLocalBaseURI() + "]");
      uri = SNSListener.getLocalBaseURI();
    }

    try {
      _server = SNSListener.startServer(uri, this);
    } catch (IOException e) {
      LOGGER.error( "There was an error starting the SNS Listener.", e);
    }

    Client client = ClientBuilder.newClient();
    target = client.target(uri);

    try {
      subscribe();
    } catch (URISyntaxException | TransportConfigException e) {
      LOGGER.error( "Error subscribing to SNS topic.");
      LOGGER.error( "Amazon SNS transport failed startup - shuting down SNS listener.", e);
      if (_server != null)
        _server.shutdownNow();
    }

  }

  @Override
  public void initialize(ProbeProcessor p, String propertiesFilename) throws TransportConfigException {
    this._processor = p;
    processPropertiesFile(propertiesFilename);
    if (_argoTopicName == null || _amazonAK == null || _amazonSK == null)
      throw new TransportConfigException("The Argo TopicName, AK and/or the SK was not specified.");

    initializeAWSClient(_amazonAK, _amazonSK);
  }

  @Override
  public void shutdown() {
    LOGGER.info("SNSTransport shutting down URL [" + _listenerURL + "] for processor [" + _processor.getRuntimeID() + "]");
    _inShutdown = true;
    unsubscribe();
    if (_server != null)
      _server.shutdownNow();
  }

  @Override
  public String transportName() {
    return this.getClass().getName();
  }

  private URI getBaseListenerURI() throws URISyntaxException {
    URI url;
    if (_listenerURL == null) {
      url = SNSListener.getLocalBaseURI();
    } else {
      url = new URI(_listenerURL);
    }
    return url;

  }

  private URI getBaseSubscriptionURI() throws URISyntaxException {
    URI url;
    if (_subscriptionURL == null) {
      url = getBaseListenerURI();
    } else {
      url = new URI(_subscriptionURL);
    }
    return url;

  }

  /**
   * Attempt to subscript to the Argo SNS topic.
   * 
   * @throws URISyntaxException if the subscription HTTP URL is messed up
   * @throws TransportConfigException if there was some Amazon specific issue
   *           while subscribing
   */
  public void subscribe() throws URISyntaxException, TransportConfigException {

    /*
     * if this instance of the transport (as there could be several - each with
     * a different topic) is in shutdown mode then don't subscribe. This is a
     * side effect of when you shutdown a sns transport and the listener gets an
     * UnsubscribeConfirmation. In normal operation, when the topic does
     * occasional house keeping and clears out the subscriptions, running
     * transports will just re-subscribe.
     */
    if (_inShutdown)
      return;

    URI url = getBaseSubscriptionURI();

    String subscriptionURL = url.toString() + "listener/sns";
    LOGGER.info("Subscription  URI - " + subscriptionURL);

    SubscribeRequest subRequest = new SubscribeRequest(_argoTopicName, "http", subscriptionURL);
    try {
      getSNSClient().subscribe(subRequest);
    } catch (AmazonServiceException e) {
      throw new TransportConfigException("Error subscribing to SNS topic.", e);
    }
    // get request id for SubscribeRequest from SNS metadata
    this._subscriptionArn = getSNSClient().getCachedResponseMetadata(subRequest).toString();
    LOGGER.info("SubscribeRequest - " + _subscriptionArn);
  }

  private void unsubscribe() {
    _snsClient.unsubscribe(_argoTopicName);
  }

  private void initializeAWSClient(String ak, String sk) {
    AWSCredentials creds = new BasicAWSCredentials(ak, sk);
    setSNSClient(new AmazonSNSClient(creds));
  }

  public AmazonSNSClient getSNSClient() {
    return _snsClient;
  }

  public ProbeProcessor getProcessor() {
    return _processor;
  }

  private void setSNSClient(AmazonSNSClient snsClient) {
    this._snsClient = snsClient;
  }

  private Properties processPropertiesFile(String propertiesFilename) throws TransportConfigException {
    Properties prop = new Properties();

    InputStream is = null;
    try {
      if (Responder.class.getResource(propertiesFilename) != null) {
        is = Responder.class.getResourceAsStream(propertiesFilename);
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

    _subscriptionURL = prop.getProperty("subscriptionURL");
    _listenerURL = prop.getProperty("listenerURL");

    _argoTopicName = prop.getProperty("argoTopicName");
    _amazonAK = prop.getProperty("amazonAK");
    _amazonSK = prop.getProperty("amazonSK");

    return prop;

  }

}
