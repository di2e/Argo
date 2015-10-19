package ws.argo.plugin.transport.sender;

/**
 * 
 * @author jmsimpson
 *
 */
public class TransportException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -2192068060934856207L;

  public TransportException() {
    super();
  }

  public TransportException(String message) {
    super(message);
  }

  public TransportException(Throwable cause) {
    super(cause);
  }

  public TransportException(String message, Throwable cause) {
    super(message, cause);
  }

  public TransportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
