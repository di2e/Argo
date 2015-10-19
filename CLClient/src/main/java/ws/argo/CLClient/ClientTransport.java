package ws.argo.CLClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ws.argo.plugin.transport.sender.Transport;
import ws.argo.plugin.transport.sender.TransportConfigException;
import ws.argo.probe.ProbeSender;

/**
 * The ClientTransport is an encapsulation object that containers the
 * ProbeSenders configured for the Client.
 * 
 * @author jmsimpson
 *
 */
public class ClientTransport {

  private static final Logger    LOGGER  = Logger.getLogger(ClientTransport.class.getName());

  private TransportConfig        config;
  private Properties             transportProps;
  private ArrayList<ProbeSender> senders = new ArrayList<ProbeSender>();
  private boolean                enabled;

  // private ArrayList<Transport> transports = new ArrayList<Transport>();

  public ClientTransport(TransportConfig config) {
    this.config = config;
    enabled = config.isEnabled();
  }

  /**
   * Initialize the actual ProbeSenders.
   * 
   * @param context the context of the client application for parameters
   * @throws TransportConfigException if something goes wonky reading specific
   *           configs for each transport
   */
  public void initialize(ArgoClientContext context) throws TransportConfigException {
    transportProps = processPropertiesFile(config.getPropertiesFilename());

    if (config.usesNetworkInterface()) {
      try {
        for (String niName : context.getAvailableNetworkInterfaces(config.requiresMulticast())) {
          try {
            Transport transport = instantiateTransportClass(config.getClassname());
            transport.initialize(transportProps, niName);
            ProbeSender sender = new ProbeSender(transport);
            senders.add(sender);
          } catch (TransportConfigException e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage());
          }
        }
      } catch (SocketException e) {
        throw new TransportConfigException("Error getting available network interfaces", e);
      }
    } else {
      Transport transport = instantiateTransportClass(config.getClassname());
      transport.initialize(transportProps, "");
      ProbeSender sender = new ProbeSender(transport);
      senders.add(sender);
    }

  }

  public String getName() {
    return config.getName();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public ArrayList<ProbeSender> getSenders() {
    return senders;
  }

  /**
   * return the text description of the transport and its associated
   * ProbeSenders.
   * 
   * @return text description (one liner)
   */
  public String getDescription() {
    StringBuffer buf = new StringBuffer();
    String transportName = config.getName();
    for (ProbeSender sender : senders) {
      buf.append(transportName).append(" -- ").append(config.isEnabled() ? "Enabled" : "Disabled")
          .append(" -- ").append(sender.getDescription());
    }
    return buf.toString();

  }

  /**
   * Show the configuration for a Client Transport.
   * 
   * @return the configuration description of the ClientTransport
   */
  public String showConfiguration() {
    StringBuffer buf = new StringBuffer();
    buf.append("\n  Client Transport Configuration\n");
    buf.append("     Name ...................... ").append(config.getName()).append("\n");
    buf.append("     Is Enabled................. ").append(config.isEnabled()).append("\n");
    buf.append("     Required Multicast ........ ").append(config.requiresMulticast()).append("\n");
    buf.append("     Uses NI ................... ").append(config.usesNetworkInterface()).append("\n");
    buf.append("     Classname ................. ").append(config.getClassname()).append("\n");
    buf.append("     Properties filename ....... ").append(config.getPropertiesFilename()).append("\n\n");
    buf.append("     Number of ProbeSenders .... ").append(this.getSenders().size()).append("\n");
    buf.append("     ProbeSenders .............. \n");
    for (ProbeSender s : getSenders()) {
      buf.append("     .... ").append(s.getDescription()).append("\n");
    }
    buf.append("  -------------------------------\n");

    return buf.toString();

  }

  private Transport instantiateTransportClass(String classname) throws TransportConfigException {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    Class<?> transportClass;
    try {
      transportClass = cl.loadClass(classname);
    } catch (ClassNotFoundException e1) {
      throw new TransportConfigException("Error loading the Transport class [" +classname + "]", e1);
    }

    Transport transport;

    try {
      transport = (Transport) transportClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.warning("Could not create an instance of the configured transport class [" + classname +"]");
      throw new TransportConfigException("Error instantiating the transport class [" + classname +"]", e);
    }
    return transport;

  }

  private static Properties processPropertiesFile(String propertiesFilename) throws TransportConfigException {
    Properties prop = new Properties();

    InputStream is = null;
    try {
      if (ArgoClient.class.getResource(propertiesFilename) != null) {
        is = ArgoClient.class.getResourceAsStream(propertiesFilename);
        LOGGER.info("Reading Argo Client properties file [" + propertiesFilename + "] from classpath.");
      } else {
        is = new FileInputStream(propertiesFilename);
        LOGGER.info("Reading Argo Client properties file [" + propertiesFilename + "] from file system.");
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

    return prop;
  }
}
