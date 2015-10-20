package ws.argo.CLClient;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.WebTarget;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.config.ClientConfiguration;
import ws.argo.plugin.transport.sender.TransportConfigException;
import ws.argo.probe.Probe;

/**
 * The ArgoClientContext class encapsulated any state that the
 * {@link ArgoClient} needs to operate at runtime. Specifically it stores setup
 * probes and the network interface list.
 * 
 * @author jmsimpson
 *
 */
public class ArgoClientContext extends CLIContext {

  private static final Logger LOGGER = Logger.getLogger(ArgoClientContext.class.getName());

  public static final String DEFAULT_CID = "ARGO-CommandLineClient";

  private String _defaultCID = DEFAULT_CID;

  private HashSet<String>        _niList = new HashSet<String>();
  private HashMap<String, Probe> _probes = new HashMap<String, Probe>();

  // Index is the probe ID
  private HashMap<String, ProbeSentRecord> _sentProbes = new HashMap<String, ProbeSentRecord>();

  private ArrayList<ClientProbeSenders> _clientSenders = new ArrayList<ClientProbeSenders>();

  /**
   * Create a new instance of the ArgoClientContext with its parent CL UI app.
   * 
   * @param app its parent CL UI app
   */
  public ArgoClientContext(ArgoClient app) {

    super(app);

    initializeLocalhostNI();
    initializeTransports(getConfig().getTransportConfigs());

  }
  
  public ClientConfiguration getConfig() {
    return ((ArgoClient) getHostApplication()).getConfig();
  }

  /**
   * Initializes the Transports.
   * 
   * @param transportConfigs the list if the transport configurations to use
   */
  private void initializeTransports(ArrayList<TransportConfig> transportConfigs) {
    for (TransportConfig config : transportConfigs) {
      ClientProbeSenders transport = new ClientProbeSenders(config);
      try {
        transport.initialize(this);
        _clientSenders.add(transport);
      } catch (TransportConfigException e) {
        LOGGER.log(Level.WARNING, "Transport [" + config.getName() + "] failed to initialize.", e);
        Console.error("Unable to initialize the Transport [" + config.getName() + "].  Ignoring.");
      }
    }

  }

  public ArrayList<ClientProbeSenders> getClientTransports() {
    return _clientSenders;
  }


  /**
   * Listener HTTP binding URL (to launch Jersey server).
   * 
   * @return Listener HTTP binding URL
   */
  public String getListenerURL() {
    return getConfig().getListenerURL();
  }

  /**
   * RespondTo HTTP binding URL.
   * 
   * @return Listener HTTP binding URL
   */
  public String getRespondToURL() {
    return getConfig().getResponseURL();
  }

  public HashMap<String, Probe> getProbes() {
    return _probes;
  }

  public Probe getProbe(String name) {
    HashMap<String, Probe> probes = getProbes();
    return probes.get(name);
  }

  public void storeProbe(String name, Probe probe) {
    getProbes().put(name, probe);
  }

  public String getDefaultCID() {
    return _defaultCID;
  }

  public void setDefaultCID(String cid) {
    _defaultCID = cid;
  }

  public Set<String> getNIList() {
    return _niList;
  }
  
  public void resetNIList() {
    _niList = new HashSet<String>();
    initializeLocalhostNI();
  }

  public WebTarget getListenerTarget() {
    return (WebTarget) this.getObject("listener");
  }

  public Map<String, ProbeSentRecord> getSentProbes() {
    return _sentProbes;
  }

  public void addSentProbe(String name, ProbeSentRecord psr) {
    _sentProbes.put(name, psr);
  }

  /**
   * Sets up the network interface list with at least the localhost NI.
   */
  private void initializeLocalhostNI() {
    InetAddress localhost;
    try {
      localhost = InetAddress.getLocalHost();
      NetworkInterface ni = NetworkInterface.getByInetAddress(localhost);

      if (ni != null)
        getNIList().add(ni.getName());

    } catch (UnknownHostException | SocketException e) {
      Console.severe("Cannot get the Network Interface for localhost");
      Console.severe(e.getMessage());
    }

  }

  /**
   * This gets a list of all the available network interface names.
   * 
   * @param requiresMulticast return only NIs that are multicast capable
   * 
   * @return the list of the currently available network interface names
   * @throws SocketException if the
   *           {@linkplain NetworkInterface#getNetworkInterfaces()} call fails
   */
  public List<String> getAvailableNetworkInterfaces(boolean requiresMulticast) throws SocketException {

    Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

    List<String> multicastNIs = new ArrayList<String>();

    // Console.info("Available Multicast-enabled Network Interfaces");
    while (nis.hasMoreElements()) {
      NetworkInterface ni = nis.nextElement();
      if (ni.isUp() && !ni.isLoopback()) {
        if (requiresMulticast) {
          if (ni.supportsMulticast())
            multicastNIs.add(ni.getName());
        } else {
          multicastNIs.add(ni.getName());
        }
      }
    }

    return multicastNIs;

  }

  /**
   * Returns the client transport specified by given name.
   * @param transportName client transport name
   * @return the ClientTransport that matches the name
   */
  public ClientProbeSenders getClientTransportNamed(String transportName) {
    
    ClientProbeSenders found = null;
    
    for (ClientProbeSenders t : _clientSenders) {
      if (t.getName().equalsIgnoreCase(transportName)) {
        found = t;
        break;
      }
    }
    
    return found;
  }
  
  public void restartListener(String url) {
    ((ArgoClient) getHostApplication()).restartListener(url);
  }

}
