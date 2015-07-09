package ws.argo.CLClient;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.sun.jersey.api.client.WebResource;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.CommandLineApplication;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeGenerator;

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
    initializeLocalhostNI();
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

  public ProbeGenerator getProbeGenerator() {
    ArgoClient client = (ArgoClient) this.getHostApplication();
    return client.getProbeGenerator();
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

}
