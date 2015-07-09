package ws.argo.MCGateway.comms;

import java.io.IOException;
import java.net.NetworkInterface;

/**
 * Class to test multicast comms in target network.
 * 
 * @author jmsimpson
 *
 */
public class MCastMultihomeResponder extends MCastMultihome {

  public static void main(String[] args) throws IOException {
    MCastMultihomeResponder r = new MCastMultihomeResponder();
    r.run(args);
  }

  @Override
  void launchOnNetowrkInterface(NetworkInterface xface) {
    System.out.println("Launching Responder for " + xface.getName());
    new MCastMultihomeResponderThread(xface, this.maddr, this.mport).start();
  }

  @Override
  String lauchHeaderTitleString() {
    return "Launching the MCast Multihome Responder";
  }
}
