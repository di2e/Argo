package ws.argo.probe;

/**
 * 
 * @author jmsimpson
 *
 */
public class ProbeSenderException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -6382572996472308231L;

  public ProbeSenderException() {
  }

  public ProbeSenderException(String message) {
    super(message);    // TODO Auto-generated constructor stub
  }

  public ProbeSenderException(Throwable cause) {
    super(cause);
  }

  public ProbeSenderException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProbeSenderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
