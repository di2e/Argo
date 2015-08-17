package ws.argo.CLClient;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jersey.api.client.WebResource;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.CommandLineApplication;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGenerator;
import ws.argo.probe.ProbeGeneratorException;
import ws.argo.probe.ProbeGeneratorFactory;

/**
 * The ArgoClientContext class encapsulated any state that the
 * {@link ArgoClient} needs to operate at runtime. Specifically it stores setup
 * probes and the network interface list.
 * 
 * @author jmsimpson
 *
 */
public class ArgoClientContext extends CLIContext {

  private boolean useMulticast = true;
  private boolean useSNS       = true;

  public static final String DEFAULT_CID = "ARGO-CommandLineClient";
  public static final String DEFAULT_ARN = "arn:aws:sns:us-east-1:627164602268:argoDiscoveryProtocol";

  /**
   * Create a new instance of the ArgoClientContext with its parent CL UI app.
   * 
   * @param app its parent CL UI app
   */
  public ArgoClientContext(CommandLineApplication<? extends CLIContext> app) {
    super(app);
    this.put("probes", new HashMap<String, Probe>());
    this.put("mcProbeGenerators", new HashMap<String, ProbeGenerator>());
    this.put("snsProbeGenerators", new HashMap<String, ProbeGenerator>());
    this.put("sentProbes", new HashMap<String, ProbeSentRecord>()); // Index is
                                                                    // the probe
                                                                    // ID
    this.put("ni-list", new ArrayList<String>());
    this.put("use-list", new ArrayList<String>());
    this.put("defaultCID", DEFAULT_CID);

    ((ArgoClient) app).getProperties().put("arn", DEFAULT_ARN);

    // initialize these values from the command line args of the client
    useMulticast = (boolean) ((ArgoClient) app).getProperties().get("useMC");
    useSNS = (boolean) ((ArgoClient) app).getProperties().get("useSNS");

    if (isUseSNS()) {
      if (getAccessKey() != null && getSecretKey() != null) {
        initializeSNSProbeGenerator();
      } else {
        Console.error("Attempting to initialize SNS generator with no Amazon Keys set (use -ak and -sk switches)");
        Console.error("Setting useSNS to false.");
        useSNS = false;
      }
    }

    try {
      initializeMCProbeGenerators();
      initializeLocalhostNI();
    } catch (SocketException e) {
      Console.error("Issue initializing the MC Probe Generators");
      e.printStackTrace();
      // TODO: do something better here
    }
  }

  /**
   * Initialize the probe generators the client can use to send probes. This
   * variants uses the instance variables to determine which transports to use.
   * 
   * @throws SocketException if something went wrong
   */
  public void initializeMCProbeGenerators() throws SocketException {
    Map<String, ProbeGenerator> probeGens = getMCProbeGenerators();

    ProbeGenerator gen;

    for (String niName : getAvailableNetworkInterfaces()) {
      try {
        gen = ProbeGeneratorFactory.createMulticastProbeGenerator(niName);
        probeGens.put(niName, gen);
      } catch (ProbeGeneratorException e) {
        Console.error("Issue creating ProbeGenerator for network interface named [" + niName + "] - " + e.getMessage());
      }

    }

  }

  /**
   * Initialize the SNS probe generator the client can use to send probes.
   * 
   */
  public void initializeSNSProbeGenerator() {

    Map<String, ProbeGenerator> probeGens = getSNSProbeGenerators();

    if (isUseSNS()) {
      try {
        ProbeGenerator gen = ProbeGeneratorFactory.createSNSProbeGenerator(getAccessKey(), getSecretKey());
        probeGens.put("sns", gen);
      } catch (ProbeGeneratorException e) {
        Console.error("Issue creating ProbeGenerator for Amazon SNS  - " + e.getMessage());
      }
    }

  }

  public boolean isUseMulticast() {
    return useMulticast;
  }

  /**
   * 
   * @param useMulticast flag for using multicast
   */
  public void setUseMulticast(boolean useMulticast) {
    this.useMulticast = useMulticast;

    if (useMulticast) {
      try {
        initializeMCProbeGenerators();
        initializeLocalhostNI();
      } catch (SocketException e) {
        Console.error("Issue initializing the MC Probe Generators");
        e.printStackTrace();
        // TODO: do something better here
      }
    }

  }

  public boolean isUseSNS() {
    return useSNS;
  }

  public void setUseSNS(boolean useSNS) {

    this.useSNS = useSNS;
  }

  /**
   * AWS Access Key.
   * 
   * @return AWS Access Key
   */
  public String getAccessKey() {
    return ((ArgoClient) getHostApplication()).getProperties().getProperty("ak");
  }

  public void setAccessKey(String key) {
    ((ArgoClient) getHostApplication()).getProperties().put("ak", key);
  }

  /**
   * AWS Secret Key.
   * 
   * @return AWS Secret Key
   */
  public String getSecretKey() {
    return ((ArgoClient) getHostApplication()).getProperties().getProperty("sk");
  }

  public void setSecretKey(String key) {
    ((ArgoClient) getHostApplication()).getProperties().put("sk", key);
  }

  /**
   * Multicast port from command line to override default.
   * 
   * @return Multicast port
   */
  public String getMulticastPort() {
    return ((ArgoClient) getHostApplication()).getProperties().getProperty("mp");
  }

  public void setMulticastPort(String port) {
    ((ArgoClient) getHostApplication()).getProperties().put("mp", port);
  }

  /**
   * Multicast address from command line to override default.
   * 
   * @return Multicast address
   */
  public String getMulticastAddress() {
    return ((ArgoClient) getHostApplication()).getProperties().getProperty("ma");
  }

  public void setMulticastAddress(String addr) {
    ((ArgoClient) getHostApplication()).getProperties().put("ma", addr);
  }

  /**
   * Listener HTTP binding URL (to launch Jersey server).
   * 
   * @return Listener HTTP binding URL
   */
  public String getURL() {
    return ((ArgoClient) getHostApplication()).getProperties().getProperty("url");
  }

  /**
   * HTTP URL for SNS Topic ARN.
   * 
   * @return SNS Topic ARN
   */
  public String getSNSTopicARN() {
    return ((ArgoClient) getHostApplication()).getProperties().getProperty("arn");
  }

  public void setSNSTopicARN(String arn) {
    ((ArgoClient) getHostApplication()).getProperties().put("arn", arn);
  }

  @SuppressWarnings("unchecked")
  public HashMap<String, Probe> getProbes() {
    return (HashMap<String, Probe>) this.getObject("probes");
  }

  public Probe getProbe(String name) {
    HashMap<String, Probe> probes = getProbes();
    return probes.get(name);
  }

  public void storeProbe(String name, Probe probe) {
    getProbes().put(name, probe);
  }

  public String getDefaultCID() {
    return this.getString("defaultCID");
  }

  public void setDefaultCID(String cid) {
    this.put("defaultCID", cid);
  }

  @SuppressWarnings("unchecked")
  public List<String> getNIList() {
    return (List<String>) this.getObject("ni-list");
  }

  public void setNIList(List<String> niList) {
    this.put("ni-list", niList);
  }

  public WebResource getListenerTarget() {
    return (WebResource) this.getObject("listener");
  }

  @SuppressWarnings("unchecked")
  public Map<String, ProbeGenerator> getMCProbeGenerators() {
    return (Map<String, ProbeGenerator>) this.getObject("mcProbeGenerators");
  }

  @SuppressWarnings("unchecked")
  public Map<String, ProbeGenerator> getSNSProbeGenerators() {
    return (Map<String, ProbeGenerator>) this.getObject("snsProbeGenerators");
  }

  /**
   * Get all of the configured Probe Generators. All of the MC Probe Generators
   * are already created (1 per Network Interface). The SNS generator may or may
   * not be created yet.
   * 
   * @return the merged Map of the MC and SNS Probe Generators
   */
  public Map<String, ProbeGenerator> getProbeGenerators() {
    Map<String, ProbeGenerator> mergedMap = new HashMap<String, ProbeGenerator>();
    
    Map<String, ProbeGenerator> mcPGs = getMCProbeGenerators();
    for (String niName : getNIList()) {
      if (mcPGs.containsKey(niName))
        mergedMap.put(niName, mcPGs.get(niName));
    }
    
    if (isUseSNS())
      mergedMap.putAll(getSNSProbeGenerators());

    return mergedMap;
  }

  @SuppressWarnings("unchecked")
  public Map<String, ProbeSentRecord> getSentProbes() {
    return (Map<String, ProbeSentRecord>) this.getObject("sentProbes");
  }

  @SuppressWarnings("unchecked")
  public void addSentProbe(String name, ProbeSentRecord psr) {
    ((Map<String, ProbeSentRecord>) this.getObject("sentProbes")).put(name, psr);
  }

  /**
   * Return the ProbeGenerator for a given Network Interface name.
   * 
   * <p>
   * All of the probe generators should be pre-created and attached to the
   * correct NI.
   * 
   * @param niName the name of the Network Interface.
   * @return the instance of the ProbeGenerator
   */
  public ProbeGenerator getProbeGeneratorForNI(String niName) {
    ProbeGenerator probeGen = null;
    if (getProbeGenerators().containsKey(niName))
      probeGen = getProbeGenerators().get(niName);
    return probeGen;
  }

  private void initializeLocalhostNI() {
    InetAddress localhost;
    try {
      localhost = InetAddress.getLocalHost();
      NetworkInterface ni = NetworkInterface.getByInetAddress(localhost);

      List<String> niList = new ArrayList<String>();
      niList.add(ni.getName());

      this.put("ni-list", niList);

    } catch (UnknownHostException | SocketException e) {
      Console.severe("Cannot get the Network Interface for localhost");
      Console.severe(e.getMessage());
    }

  }

  private List<String> getAvailableNetworkInterfaces() throws SocketException {

    Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

    List<String> multicastNIs = new ArrayList<String>();

    // Console.info("Available Multicast-enabled Network Interfaces");
    while (nis.hasMoreElements()) {
      NetworkInterface ni = nis.nextElement();
      if (ni.isUp() && ni.supportsMulticast() && !ni.isLoopback())
        multicastNIs.add(ni.getName());
    }

    return multicastNIs;

  }

}
