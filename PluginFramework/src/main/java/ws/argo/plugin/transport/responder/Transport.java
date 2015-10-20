package ws.argo.plugin.transport.responder;

/**
 * The Transport interfaces defines the API for an Argo Responder transport. The
 * idea is that a transport will be run in its own thread - therefore the
 * Runnable declaration.
 * 
 * @author jmsimpson
 *
 */
public interface Transport extends Runnable {

  void initialize(ProbeProcessor p, String propertiesFilename) throws TransportConfigException;

  public void shutdown();

  public String transportName();

}
