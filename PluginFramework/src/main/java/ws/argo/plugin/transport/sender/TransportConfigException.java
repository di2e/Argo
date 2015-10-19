package ws.argo.plugin.transport.sender;

/**
 * This exception gets thrown when some kind of error occured during the
 * configuration of a Transport.
 * 
 * @author jmsimpson
 *
 */
public class TransportConfigException extends Exception {

  private static final long serialVersionUID = 753487924802419379L;

  public TransportConfigException() {
    super();
  }

  public TransportConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  public TransportConfigException(String message) {
    super(message);
  }
}
