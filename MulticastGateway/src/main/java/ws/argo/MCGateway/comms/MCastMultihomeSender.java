package ws.argo.MCGateway.comms;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class MCastMultihomeSender extends MCastMultihome {
  Integer numMsgs;

  public static void main(String[] args) throws IOException {
    MCastMultihomeSender r = new MCastMultihomeSender();
    r.run(args);
  }

  @Override
  void launchOnNetowrkInterface(NetworkInterface xface) {
    System.out.println("Launching Sender for " + xface.getDisplayName());
    new MCastMultihomeSenderThread(xface, this.maddr, this.mport, this.numMsgs).start();
  }

  protected void setCliValues(Properties cliValues) {
    super.setCliValues(cliValues);
    this.numMsgs = (Integer) cliValues.get("nm");
    if (this.numMsgs == null)
      this.numMsgs = 0;
  }

  protected Properties processCommandLine(CommandLine cl) throws RuntimeException {
    Properties p = super.processCommandLine(cl);

    // Number of messages to send
    if (cl.hasOption("nm")) {
      try {
        Integer numMsgs = Integer.valueOf(cl.getOptionValue("nm"));
        p.put("nm", numMsgs);
      } catch (NumberFormatException e) {
        throw new RuntimeException("The numMsgs number - " + cl.getOptionValue("nm") + " - is not formatted as an integer", e);
      }
    }
    return p;
  }

  @Override
  String lauchHeaderTitleString() {
    return "Launching the MCast Multihome Sender";
  }

  @SuppressWarnings("static-access")
  Options getOptions() {

    super.getOptions();
    options.addOption(OptionBuilder.withArgName("numMsgs").hasArg().withType(new Integer(0)).withDescription("number of messages to send").create("nm"));
    return options;
  }
}
