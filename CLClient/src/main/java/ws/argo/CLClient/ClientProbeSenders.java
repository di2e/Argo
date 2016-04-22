package ws.argo.CLClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;
import ws.argo.plugin.transport.sender.Transport;
import ws.argo.probe.ProbeSender;

/**
 * The ClientTransport is an encapsulation object that containers the
 * ProbeSenders configured for the Client.
 * 
 * There is an array of ProbeSender if and only if the Transport depends on a
 * set of Network Interfaces. If not, then there is only one ProbeSender.
 * 
 * @author jmsimpson
 *
 */
public class ClientProbeSenders {

  private static final Logger    LOGGER  = LogManager.getLogger(ClientProbeSenders.class.getName());

  private TransportConfig        config;
  private Properties             transportProps;
  private ArrayList<ProbeSender> senders;
  private boolean                enabled;

  public ClientProbeSenders(TransportConfig config) {
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

    createProbeSenders(context);

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
      buf.append(transportName).append(" -- ").append(isEnabled() ? "Enabled" : "Disabled")
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
    buf.append("     Is Enabled................. ").append(isEnabled()).append("\n");
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

  /**
   * This closes any of the underlying resources that a ProbeSender might be
   * using, such as a connection to a server somewhere.
   */
  public void close() {
    for (ProbeSender sender : getSenders()) {
      try {
        sender.close();
      } catch (TransportException e) {
        LOGGER.warn( "Issue closing the ProbeSender [" + sender.getDescription() + "]", e);
      }
    }
  }

  /**
   * Restart the Senders. This will use whatever transport configuration was
   * already loaded in at
   * 
   * @param context the context of the client application for parameters
   * @throws TransportConfigException if something goes wonky reading specific
   *           configs for each transport
   */
  public void restart(ArgoClientContext context) throws TransportConfigException {
    createProbeSenders(context);
  }

  /**
   * Reload the transport configuration files and restart the ProbeSenders.
   * 
   * @param context the context of the client application for parameters
   * @throws TransportConfigException if something goes wonky reading specific
   *           configs for each transport
   */
  public void reloadAndRestart(ArgoClientContext context) throws TransportConfigException {
    initialize(context);
  }

  /**
   * Create the actual ProbeSender instances given the configuration
   * information. If the transport depends on Network Interfaces, then create a
   * ProbeSender for each NI we can find on this machine.
   * 
   * @param context the main client configuration information
   * @throws TransportConfigException if there is some issue initializing the
   *           transport.
   */
  private void createProbeSenders(ArgoClientContext context) throws TransportConfigException {
    
    senders = new ArrayList<ProbeSender>();
    
    if (config.usesNetworkInterface()) {
      try {
        for (String niName : context.getAvailableNetworkInterfaces(config.requiresMulticast())) {
          try {
            Transport transport = instantiateTransportClass(config.getClassname());
            transport.initialize(transportProps, niName);
            ProbeSender sender = new ProbeSender(transport);
            senders.add(sender);
          } catch (TransportConfigException e) {
            LOGGER.warn( e.getLocalizedMessage());
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

  private Transport instantiateTransportClass(String classname) throws TransportConfigException {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    Class<?> transportClass;
    try {
      transportClass = cl.loadClass(classname);
    } catch (ClassNotFoundException e1) {
      throw new TransportConfigException("Error loading the Transport class [" + classname + "]", e1);
    }

    Transport transport;

    try {
      transport = (Transport) transportClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.warn("Could not create an instance of the configured transport class [" + classname + "]");
      throw new TransportConfigException("Error instantiating the transport class [" + classname + "]", e);
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
