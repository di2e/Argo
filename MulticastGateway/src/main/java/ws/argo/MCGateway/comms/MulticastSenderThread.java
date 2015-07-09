package ws.argo.MCGateway.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Class to test multicast comms in target network.
 * 
 * @author jmsimpson
 *
 */
public class MulticastSenderThread implements Runnable {

  // static String multicastGroup = "239.255.0.1";
  static String multicastGroup = "230.0.0.2";
  // static String multicastGroup = "FF0E::230:1";

  /**
   * Run the sender thread.
   */
  @SuppressWarnings("resource")
  public void run() {
    // TODO Auto-generated method stub

    MulticastSocket socket = null;
    try {
      socket = new MulticastSocket(4018);
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    InetAddress group = null;
    try {
      group = InetAddress.getByName(multicastGroup);
    } catch (UnknownHostException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    while (true) {
      try {
        byte[] buf = new byte[256];
        // don't wait for request...just send a quote

        String dString = "This is a message sent at " + new Date().toString();

        buf = dString.getBytes();

        DatagramPacket packet;
        packet = new DatagramPacket(buf, buf.length, group, 4003);
        socket.send(packet);

        System.out.println("Sent: " + dString);

        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {}
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

}
