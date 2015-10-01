package ws.argo.CLClient;

/**
 * This exception is thrown when a generic configuration exception occurs in the
 * Client.
 * 
 * @author jmsimpson
 *
 */
public class ArgoClientConfigException extends Exception {

  private static final long serialVersionUID = 5456181894494745935L;

  public ArgoClientConfigException() {
    super();
  }

  public ArgoClientConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  public ArgoClientConfigException(String message) {
    super(message);
  }

}
