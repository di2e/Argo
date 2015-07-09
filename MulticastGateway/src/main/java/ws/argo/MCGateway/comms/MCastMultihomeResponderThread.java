package ws.argo.MCGateway.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MCastMultihomeResponderThread extends Thread {

  InetSocketAddress saddr;
  NetworkInterface  ni;
  MulticastSocket   socket             = null;
  String            maddr;
  Integer           mport;
  String            addrsDisplayString = null;

  public MCastMultihomeResponderThread(NetworkInterface n, String maddr, Integer mport) {
    this.ni = n;
    this.maddr = maddr;
    this.mport = mport;
  }

  boolean joinGroup() {
    boolean success = true;
    InetSocketAddress socketAddress = new InetSocketAddress(maddr, mport);
    try {
      // System.out.println(this.ni.getName()+" joining group
      // "+socketAddress.toString());
      this.socket = new MulticastSocket(mport);
      socket.joinGroup(socketAddress, ni);
      System.out.println(this.ni.getName() + " joined group " + socketAddress.toString());
    } catch (IOException e) {
      StringBuffer buf = new StringBuffer();
      try {
        buf.append("(lb:" + this.ni.isLoopback() + " ");
      } catch (SocketException e2) {
        buf.append("(lb:err ");
      }
      try {
        buf.append("m:" + this.ni.supportsMulticast() + " ");
      } catch (SocketException e3) {
        buf.append("(m:err ");
      }
      try {
        buf.append("p2p:" + this.ni.isPointToPoint() + " ");
      } catch (SocketException e1) {
        buf.append("p2p:err ");
      }
      try {
        buf.append("up:" + this.ni.isUp() + " ");
      } catch (SocketException e1) {
        buf.append("up:err ");
      }
      buf.append("v:" + this.ni.isVirtual() + ") ");

      System.out.println(this.ni.getName() + " " + buf.toString() + ": could not join group " + socketAddress.toString() + " --> " + e.toString());
      //
      // e.printStackTrace();
      success = false;
    }
    return success;
  }

  String nicAddrDisplayString() {

    if (addrsDisplayString != null)
      return addrsDisplayString;

    Enumeration<InetAddress> addrs = this.ni.getInetAddresses();
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    while (addrs.hasMoreElements()) {
      InetAddress addr = addrs.nextElement();
      buf.append(addr.toString()).append(" ");
    }
    buf.append("]");

    addrsDisplayString = buf.toString();

    return addrsDisplayString;

  }

  @Override
  public void run() {

    if (!joinGroup())
      return;

    DatagramPacket packet;
    // infinite loop until the responder is terminated
    while (true) {

      byte[] buf = new byte[1024];
      packet = new DatagramPacket(buf, buf.length);

      // System.out.println("Listening for packet on interface
      // "+this.ni.getName()+":"+this.ni.getDisplayName()+":"+nicAddrDisplayString());
      try {
        socket.receive(packet);
        // System.out.println("Received packet on interface
        // "+this.ni.getName()+":"+this.ni.getDisplayName());
        // Get the string
        String probeStr = new String(packet.getData(), 0, packet.getLength());
        System.out.println(this.ni.getName() + " -->:   " + probeStr);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

  }

}
