package ws.argo.MCGateway;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The GRHandlerThread actually processes inbound TCP packets from a paired
 * sender that is fielding UDP multicast packets on another network.  
 * 
 * <p>
 * It is implemented as a thread to allow scalability for processing.
 * 
 * @author jmsimpson
 *
 */
public class GRHandlerThread extends Thread {
  private static final Logger LOGGER = Logger.getLogger(GRHandlerThread.class.getName());

  Socket          s;
  Boolean         repeat;
  MulticastSocket outboundSocket;
  InetAddress     maddress;
  Integer         multicastPort;

  /**
   * Creates an instance of the CRHandlerThread.
   * 
   * @param s the socket to read from
   * @param repeat flag to repeat the incomming message
   * @param outboundSocket socket on which to send
   * @param maddress multicast group address on which to send
   * @param multicastPort port on which to send
   */
  public GRHandlerThread(Socket s, Boolean repeat, MulticastSocket outboundSocket, InetAddress maddress, Integer multicastPort) {
    super();
    this.s = s;
    this.repeat = repeat;
    this.outboundSocket = outboundSocket;
    this.multicastPort = multicastPort;
    this.maddress = maddress;
  }

  /**
   * Process the message on the socket.
   * 
   * <p>
   * Read it off the inbound TCP socket, repackage it and send it off on the UDP
   * multicast socket.
   */
  public void run() {

    byte[] fullMessage = new byte[0];

    byte[] buffer = new byte[1024];
    int length = 0;

    // Read in whatever message is sent on the socket.
    LOGGER.fine("Reading message ...");

    try {
      do {
        length = s.getInputStream().read(buffer);

        LOGGER.fine("Read " + length + " bytes: ");

        if (length > 0) {

          byte[] destination = new byte[fullMessage.length + length];
          System.arraycopy(fullMessage, 0, destination, 0, fullMessage.length);
          System.arraycopy(buffer, 0, destination, fullMessage.length, length);

          LOGGER.fine("New appended message: " + new String(destination));

          fullMessage = destination;
        }

      } while (length != -1); // End the loop at EOF

      s.close();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error reading the message coming in on the TCP socket", e);
    }

    LOGGER.info("Received message of size " + fullMessage.length);
    if (fullMessage.length > 0) {
      LOGGER.fine("Sending message of size " + fullMessage.length + ":");
      LOGGER.fine("MESSAGE -->> " + new String(fullMessage));
      // Send out message on group address

      if (this.repeat) {
        LOGGER.fine("Repeating message @ " + outboundSocket.getLocalAddress() + ":" + multicastPort);

        DatagramPacket packet = new DatagramPacket(fullMessage, fullMessage.length, maddress, multicastPort);
        try {
          // TODO: This might want to be a configurable option.
          outboundSocket.setTimeToLive(200);
          outboundSocket.send(packet);
          LOGGER.fine("Message successfully repeated");
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, "Error sending outbound multicast message", e);
        }
      }
    }

  }

}
