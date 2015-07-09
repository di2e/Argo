package ws.argo.CLClient;

import java.io.IOException;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import net.dharwin.common.tools.cli.api.CLIContext;
import net.dharwin.common.tools.cli.api.CommandLineApplication;
import net.dharwin.common.tools.cli.api.annotations.CLIEntry;
import net.dharwin.common.tools.cli.api.console.Console;
import net.dharwin.common.tools.cli.api.exceptions.CLIInitException;
import ws.argo.CLClient.listener.ResponseListener;
import ws.argo.probe.ProbeGenerator;

/**
 * The ArgoClient represents the command line client for sending probes and
 * receiving responses from Argo Responders on the reachable network.
 * 
 * @author jmsimpson
 *
 */
@CLIEntry
public class ArgoClient extends CommandLineApplication<ArgoClientContext> {

  private static final Logger LOGGER = Logger.getLogger(ProbeGenerator.class.getName());

  private HttpServer server;

  private ProbeGenerator gen;

  public ArgoClient() throws CLIInitException {
    super();
    gen = new ProbeGenerator("230.0.0.1", 4003);
  }

  public ProbeGenerator getProbeGenerator() {
    return gen;
  }

  private void startListener() throws IOException {
    server = ResponseListener.startServer();
    WebResource target = Client.create().resource(ResponseListener.BASE_URI);
    _appContext.put("listener", target);
  }

  @Override
  public void start(String[] args) {

    startServices();
    setPrompt("Argo");
    super.start(args);

  }

  private void startServices() {
    try {
      startListener();
    } catch (IOException e) {
      Console.severe("Unable to start services.");
      e.printStackTrace();
    }
  }

  @Override
  protected void shutdown() {
    gen.close();
    server.stop();
    System.out.println("Shutting down ArgoClient.");
  }

  @Override
  protected CLIContext createContext() {
    return new ArgoClientContext(this);
  }

}
