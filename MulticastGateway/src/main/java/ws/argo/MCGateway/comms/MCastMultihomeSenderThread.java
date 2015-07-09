package ws.argo.MCGateway.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

public class MCastMultihomeSenderThread extends Thread {

  InetSocketAddress saddr;
  NetworkInterface  ni;
  MulticastSocket   socket = null;
  String            maddr;
  Integer           mport;
  Integer           numMsgs;
  String            addrsDisplayString;

  public MCastMultihomeSenderThread(NetworkInterface n, String maddr, Integer mport, Integer numMsgs) {
    this.ni = n;
    this.maddr = maddr;
    this.mport = mport;
    this.numMsgs = numMsgs;
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

    MulticastSocket socket = null;
    try {
      socket = new MulticastSocket();
      socket.setNetworkInterface(ni);
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    InetAddress group = null;
    try {
      group = InetAddress.getByName(maddr);
    } catch (UnknownHostException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    Integer msgNum = 0;

    while (this.numMsgs <= 0 || msgNum < this.numMsgs) { // if limit of numMsgs
                                                         // is 0 (or less) then
                                                         // loop forever other
                                                         // wise stop on numMsgs
      try {
        byte[] buf = new byte[512];

        String displayName = socket.getNetworkInterface().getDisplayName();
        int index = socket.getNetworkInterface().getIndex();

        String dString = "This is a message sent over " + displayName + "[" + index + "]:" + group.toString() + " responding at " + nicAddrDisplayString() + " on " + new Date().toString();

        buf = dString.getBytes();

        DatagramPacket packet;
        packet = new DatagramPacket(buf, buf.length, group, mport);
        socket.send(packet);

        System.out.println("Sent: " + dString);

        try {
          int secs = 1 + (new Random()).nextInt(4);
          Thread.sleep(secs * 1000);
        } catch (InterruptedException e) {}
        msgNum++;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

}
