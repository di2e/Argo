package ws.argo.CLClient;

import java.io.IOException;
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

/**
 * The ArgoClientContext class encapsulated any state that the
 * {@link ArgoClient} needs to operate at runtime. Specifically it stores setup
 * probes and the network interface list.
 * 
 * @author jmsimpson
 *
 */
public class ArgoClientContext extends CLIContext {

  /**
   * Create a new instance of the ArgoClientContext with its parent CL UI app.
   * 
   * @param app its parent CL UI app
   */
  public ArgoClientContext(CommandLineApplication<? extends CLIContext> app) {
    super(app);
    this.put("probes", new HashMap<String, Probe>());
    this.put("probeGenerators", new HashMap<String, ProbeGenerator>());
    this.put("ni-list", new ArrayList<String>());

    try {
      initializeProbeGenerators();
      initializeLocalhostNI();
    } catch (SocketException e) {
      Console.error("Issue initializeing the Probe Generators");
      e.printStackTrace();
    }
  }

  private void initializeProbeGenerators() throws SocketException {
    Map<String, ProbeGenerator> probeGens = getProbeGenerators();

    for (String niName : initializeAvailableNetworkInterfaces()) {
      try {
        ProbeGenerator gen = new ProbeGenerator(niName);
        probeGens.put(niName, gen);
      } catch (ProbeGeneratorException e) {
        Console.error("Issue creating ProbeGenerator for network interface named [" + niName + "]");
        e.printStackTrace();
      }

    }

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
  public Map<String, ProbeGenerator> getProbeGenerators() {
    return (Map<String, ProbeGenerator>) this.getObject("probeGenerators");
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

  private List<String> initializeAvailableNetworkInterfaces() throws SocketException {

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
