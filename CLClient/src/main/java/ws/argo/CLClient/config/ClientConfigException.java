package ws.argo.CLClient.config;

/**
 * This exception is thrown when a generic configuration exception occurs in the
 * Client.
 * 
 * @author jmsimpson
 *
 */
public class ClientConfigException extends Exception {

  private static final long serialVersionUID = 5456181894494745935L;

  public ClientConfigException() {
    super();
  }

  public ClientConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  public ClientConfigException(String message) {
    super(message);
  }

}
