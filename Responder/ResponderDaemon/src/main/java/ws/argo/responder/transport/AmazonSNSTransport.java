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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.SubscribeRequest;

import ws.argo.responder.Responder;
import ws.argo.responder.transport.sns.SNSListener;


public class AmazonSNSTransport implements Transport {

  static final String DEFAULT_TOPIC_NAME = "arn:aws:sns:us-east-1:627164602268:argoDiscoveryProtocol";
  static final String DEFAULT_AK = "AKIAJXYLKPZVDS6VDLJA";
  static final String DEFAULT_SK = "1MAA3DES3HZ/awWoNx3GLd46zcyJQLcNR3VJ720D";

  
  private static final Logger LOGGER = Logger.getLogger(AmazonSNSTransport.class.getName());

  private HttpServer server;
  WebTarget target;

  private AmazonSNSClient snsClient;
  private String argoTopicName = DEFAULT_TOPIC_NAME;

  private String subscriptionArn;

  private Properties cliValues;
  private ProbeProcessor processor;
  private String listenerURL;
  private String amazonAK;
  private String amazonSK;
  private String subscriptionURL;
  
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
      server = SNSListener.startServer(uri);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "There was an error starting the SNS Listener.", e);
    }

    Client client = ClientBuilder.newClient();
    target = client.target(uri);
    
    try {
      subscribe();
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    
  }

  @Override
  public void initialize(ProbeProcessor p, String propertiesFilename) throws TransportConfigException {
    this.processor = p;
    processPropertiesFile(propertiesFilename);
    initializeAWSClient(cliValues.getProperty("ak"), cliValues.getProperty("sk"));      
  }

  @Override
  public void shutdown() {
    LOGGER.info("SNSTransport shutting down URL [" + listenerURL + "] for processor [" + processor.getRuntimeID() + "]");
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
      url = SNSListener.getLocalBaseURI();
    } else {
      url = new URI(subscriptionURL);
    }
    return url;

  }
  
  private void subscribe() throws URISyntaxException {
    
    URI url = getBaseSubscriptionURI();
    
    String subscriptionURL = url.toString() + "/listener/sns";
    System.out.println("Subscription  URI - " + subscriptionURL);
    
    SubscribeRequest subRequest = new SubscribeRequest(argoTopicName, "http", subscriptionURL);
    getSNSClient().subscribe(subRequest);
    //get request id for SubscribeRequest from SNS metadata
    this.subscriptionArn = getSNSClient().getCachedResponseMetadata(subRequest).toString();
    System.out.println("SubscribeRequest - " + subscriptionArn);
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
//    networkInterface = prop.getProperty("networkInterface");
    
    argoTopicName = prop.getProperty("argoTopicName", DEFAULT_TOPIC_NAME);
    amazonAK = prop.getProperty("amazonAK", DEFAULT_AK);
    amazonSK = prop.getProperty("amazonSK", DEFAULT_SK);
        

    return prop;

  }

}
