package ws.argo.responder.transport;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.SubscribeRequest;

import ws.argo.plugin.transport.responder.ProbeProcessor;
import ws.argo.plugin.transport.responder.Transport;
import ws.argo.plugin.transport.responder.TransportConfigException;
import ws.argo.responder.Responder;
import ws.argo.responder.transport.sns.SNSListener;

/**
 * The AmazonSNSTransport is a transport that uses the Amazon SNS service as the
 * pub/sub mechanism to move probes around.
 * 
 * @author jmsimpson
 *
 */
public class AmazonSNSTransport implements Transport {

  private static final Logger LOGGER     = Logger.getLogger(AmazonSNSTransport.class.getName());

  private HttpServer          server;
  WebTarget                   target;

  private AmazonSNSClient     snsClient;
  private String              argoTopicName;
  private boolean             inShutdown = false;

  // Configuration params
  private String              subscriptionArn;
  private ProbeProcessor      processor;
  private String              listenerURL;
  private String              amazonAK;
  private String              amazonSK;
  private String              subscriptionURL;

  public AmazonSNSTransport() {
  }

  @Override
  public void run() {

    URI uri;

    try {
      uri = getBaseListenerURI();
    } catch (URISyntaxException e) {
      LOGGER.warning("The listenerURL specified in the configuration file [" + listenerURL + "] is invalid. ");
      LOGGER.info("Using the default listner URL assocaited with the loca host [" + SNSListener.getLocalBaseURI() + "]");
      uri = SNSListener.getLocalBaseURI();
    }

    try {
      server = SNSListener.startServer(uri, this);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "There was an error starting the SNS Listener.", e);
    }

    Client client = ClientBuilder.newClient();
    target = client.target(uri);

    try {
      subscribe();
    } catch (URISyntaxException | TransportConfigException e) {
      LOGGER.log(Level.SEVERE, "Error subscribing to SNS topic.");
      LOGGER.log(Level.SEVERE, "Amazon SNS transport failed startup - shuting down SNS listener.", e);
      if (server != null)
        server.shutdownNow();
    }

  }

  @Override
  public void initialize(ProbeProcessor p, String propertiesFilename) throws TransportConfigException {
    this.processor = p;
    processPropertiesFile(propertiesFilename);
    if (argoTopicName == null || amazonAK == null || amazonSK == null)
      throw new TransportConfigException("The Argo TopicName, AK and/or the SK was not specified.");

    initializeAWSClient(amazonAK, amazonSK);
  }

  @Override
  public void shutdown() {
    LOGGER.info("SNSTransport shutting down URL [" + listenerURL + "] for processor [" + processor.getRuntimeID() + "]");
    inShutdown = true;
    unsubscribe();
    if (server != null)
      server.shutdownNow();
  }

  @Override
  public String transportName() {
    return this.getClass().getName();
  }

  private URI getBaseListenerURI() throws URISyntaxException {
    URI url;
    if (listenerURL == null) {
      url = SNSListener.getLocalBaseURI();
    } else {
      url = new URI(listenerURL);
    }
    return url;

  }

  private URI getBaseSubscriptionURI() throws URISyntaxException {
    URI url;
    if (subscriptionURL == null) {
      url = getBaseListenerURI();
    } else {
      url = new URI(subscriptionURL);
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
    if (inShutdown)
      return;

    URI url = getBaseSubscriptionURI();

    String subscriptionURL = url.toString() + "listener/sns";
    LOGGER.info("Subscription  URI - " + subscriptionURL);

    SubscribeRequest subRequest = new SubscribeRequest(argoTopicName, "http", subscriptionURL);
    try {
      getSNSClient().subscribe(subRequest);
    } catch (AmazonServiceException e) {
      throw new TransportConfigException("Error subscribing to SNS topic.", e);
    }
    // get request id for SubscribeRequest from SNS metadata
    this.subscriptionArn = getSNSClient().getCachedResponseMetadata(subRequest).toString();
    LOGGER.info("SubscribeRequest - " + subscriptionArn);
  }

  private void unsubscribe() {
    snsClient.unsubscribe(argoTopicName);
  }

  private void initializeAWSClient(String ak, String sk) {
    AWSCredentials creds = new BasicAWSCredentials(ak, sk);
    setSNSClient(new AmazonSNSClient(creds));
  }

  public AmazonSNSClient getSNSClient() {
    return snsClient;
  }

  public ProbeProcessor getProcessor() {
    return processor;
  }

  private void setSNSClient(AmazonSNSClient snsClient) {
    this.snsClient = snsClient;
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

    subscriptionURL = prop.getProperty("subscriptionURL");
    listenerURL = prop.getProperty("listenerURL");

    argoTopicName = prop.getProperty("argoTopicName");
    amazonAK = prop.getProperty("amazonAK");
    amazonSK = prop.getProperty("amazonSK");

    return prop;

  }

}
