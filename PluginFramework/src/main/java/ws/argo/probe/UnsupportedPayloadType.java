package ws.argo.probe;

/**
 * The UnsupportedPayloadType is thrown if the payload type specified in a probe
 * is not XML or JSON.
 * 
 * <p>You kinda have to do it this way. Ideally you could have an ENUM that would
 * detect a bad payload type at compile time, but because we are dealing with
 * text here, I need to check the text and throw an exception if its not valid.
 * 
 * @author jmsimpson
 *
 */
public class UnsupportedPayloadType extends Exception {

  private static final long serialVersionUID = 346457292691086877L;

  public UnsupportedPayloadType() {
    super();
  }

  public UnsupportedPayloadType(String message) {
    super(message);
  }

}
