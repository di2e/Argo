package ws.argo.responder.transport;

/**
 * The MulticastOperationException is thrown if some operational error occurs
 * with the Multicast Transport.
 * 
 * @author jmsimpson
 *
 */
public class MulticastOperationException extends Exception {

  private static final long serialVersionUID = 5450154353983696662L;

  public MulticastOperationException() {
    super();
  }

  public MulticastOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  public MulticastOperationException(String message) {
    super(message);
  }

}
