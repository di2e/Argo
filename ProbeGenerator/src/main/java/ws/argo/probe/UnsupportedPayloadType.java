package ws.argo.probe;

public class UnsupportedPayloadType extends Exception {

  private static final long serialVersionUID = 346457292691086877L;

  public UnsupportedPayloadType() {
    super();
  }

  public UnsupportedPayloadType(String message) {
    super(message);
  }

}
