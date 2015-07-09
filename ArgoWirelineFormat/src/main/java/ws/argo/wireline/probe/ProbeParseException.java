package ws.argo.wireline.probe;

/**
 * Exception to indicate that a parsing problem happened with wireline probes.
 * 
 * @author jmsimpson
 *
 */
public class ProbeParseException extends Exception {

  /**
   * Auto generated.
   */
  private static final long serialVersionUID = 7415856451120137226L;

  public ProbeParseException() {
   
  }

  public ProbeParseException(String message) {
    super(message);
  }

  public ProbeParseException(Throwable cause) {
    super(cause);
  }

  public ProbeParseException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProbeParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
