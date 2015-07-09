package ws.argo.MCGateway.comms;

import java.io.IOException;

/**
 * Class to test multicast comms in target network.
 * 
 * @author jmsimpson
 *
 */
public class MulticastSender {

  /**
   * Runs the multicast sender.
   * 
   * @param args command line arguments
   * @throws IOException if something goes wrong
   */
  public static void main(String[] args) throws IOException {
    Thread t;

    t = new Thread(new MulticastSenderThread());

    t.start();
  }

}
