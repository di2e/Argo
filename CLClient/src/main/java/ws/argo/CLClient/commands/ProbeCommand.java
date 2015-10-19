package ws.argo.CLClient.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import net.dharwin.common.tools.cli.api.Command;
import net.dharwin.common.tools.cli.api.CommandResult;
import net.dharwin.common.tools.cli.api.CompoundCommand;
import net.dharwin.common.tools.cli.api.annotations.CLICommand;
import net.dharwin.common.tools.cli.api.console.Console;
import ws.argo.CLClient.ArgoClientContext;
import ws.argo.CLClient.ClientTransport;
import ws.argo.CLClient.ProbeSentRecord;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSender;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.wireline.probe.ProbeWrapper;

/**
 * This encapsulates all of the probe commands. New, Del, Mod, Send.
 * 
 * @author jmsimpson
 *
 */
@CLICommand(name = "probe", description = "manage probes")
public class ProbeCommand extends CompoundCommand<ArgoClientContext> {

  /**
   * This command lists all of the sent probes and the times they were sent.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "listSent" }, commandDescription = "list all the sent probes")
  public class ListSentProbes extends Command<ArgoClientContext> {

    @Parameter
    private List<String> _namePatterns = new ArrayList<String>();

    @Parameter(names = "-payload", description = "show the actual probe payload")
    private boolean      _showPayload;

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      Map<String, ProbeSentRecord> probes = context.getSentProbes();
      List<String> probeNames = new ArrayList<String>();

      if (probes.isEmpty()) {
        Console.info("No sent probes were recorded.");
      } else {
        if (_namePatterns.isEmpty()) {
          Console.info("Listing all sent probes.");
          for (String probeName : probes.keySet())
            probeNames.add(probeName);
        } else {
          Console.info("Listing sent probes with names matching: [" + _namePatterns + "]");
          for (String pattern : _namePatterns) {
            for (String probeName : probes.keySet()) {
              if (probeName.matches(pattern))
                probeNames.add(probeName);
            }
          }
        }

        DateFormat fmt = DateFormat.getDateTimeInstance();

        Console.info("Sent probes:");
        for (String probeName : probeNames) {
          ProbeSentRecord psr = probes.get(probeName);

          String dateTimeLine = fmt.format(psr.getSentDate());
          String probeInfoLine = probeInfoLine(psr.getProbe(), _showPayload);

          Console.info("Probe: NAME [" + probeName + "] | " + dateTimeLine + " | " + probeInfoLine);
        }

      }

      return CommandResult.OK;
    }

  }

  /**
   * This command class will actually send the probe.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "send" }, commandDescription = "send the probe")
  public class SendProbe extends Command<ArgoClientContext> {

    @Parameter(description = "list of probe names")
    private List<String> _probeNames = new ArrayList<>();

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      if (_probeNames.isEmpty()) {
        return sendAllProbes(context);
      } else {
        return sendProbes(context, _probeNames);
      }

    }

    private CommandResult sendProbes(ArgoClientContext context, List<String> probeNames) {
      Console.error("Sending specified list of probes.");
      for (String probeName : probeNames) {
        Probe probe = context.getProbe(probeName);
        if (probe != null) {
          sendProbe(context, probeName, probe);
        } else {
          Console.error("Unknown probe name specified [" + probeName +"]");
        }
      }
      return CommandResult.OK;
    }

    private CommandResult sendAllProbes(ArgoClientContext context) {
      Console.info("Empty list of probe names - sending all probes");
      HashMap<String, Probe> probes = context.getProbes();
      for (Entry<String, Probe> entry : probes.entrySet()) {
        sendProbe(context, entry.getKey(), entry.getValue());
      }
      return CommandResult.OK;
    }

    private CommandResult sendProbe(ArgoClientContext context, String probeName, Probe probe) {

      ArrayList<ClientTransport> transports = context.getClientTransports();

      Probe reifiedProbe;
      try {
        reifiedProbe = new Probe(probe);

        // Should this be configurable? It looks like a magic number to me, but
        // it's my magic number and not someone else's - ok, I'm rambling.
        URL rURL = new URL(context.getRespondToURL());

        String respondToURL = rURL.toString() + "/listener/probeResponse";

        reifiedProbe.addRespondToURL("argo-client", respondToURL);
      } catch (MalformedURLException | UnsupportedPayloadType e1) {
        Console.error("Probe failed [" + probeName +"]");
        Console.error(e1.getMessage());
        ProbeSentRecord psr = new ProbeSentRecord(null, e1.getMessage());
        context.addSentProbe(probeName, psr);
        return CommandResult.ERROR;
      }

      for (ClientTransport t : transports) {
        if (t.isEnabled()) {
          for (ProbeSender sender : t.getSenders()) {
            try {
              sender.sendProbe(reifiedProbe);
              Console.info("Sent probe [" + probeName + "] on [" + sender.getDescription() + "]");
              ProbeSentRecord psr = new ProbeSentRecord(reifiedProbe, "Success");
              context.addSentProbe(probeName, psr);
            } catch (ProbeSenderException e) {
              Console.error("Probe failed [" + probeName +"]");
              Console.error(e.getMessage());
              ProbeSentRecord psr = new ProbeSentRecord(reifiedProbe, e.getMessage());
              context.addSentProbe(probeName, psr);
              return CommandResult.ERROR;
            }
          }
        }
      }

      return CommandResult.OK;
    }

  }

  /**
   * This class will create a new probe that the CL UI can use over and over
   * again when sending probes.
   *
   * <p>The command structure is: create -n : for future reference and use with
   * 'send' command. -cid --clientID : sets the client ID in the probe. -scids
   * --serviceContractIDs : takes a whitespace separated list strings to use as
   * contract IDs -siids --serviceInstanceIDs : takes a whitespace separated
   * list strings to use as instance IDs -ptype --respondToPayloadType : sets
   * the respondTo payload type. Must be JSON or XML or and error happens.
   *
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "new" }, commandDescription = "Create a new probe")
  public class NewProbe extends Command<ArgoClientContext> {

    @Parameter(names = { "-n", "--name" }, description = "name of the probe for future reference.", required = false)
    private String       _probeName;

    @Parameter(names = { "-cid", "--clientID" }, description = "client ID for the probe", required = false)
    private String       _clientID;

    @Parameter(names = { "-scids", "--serviceContractIDs" }, variableArity = true)
    private List<String> _scids = new ArrayList<>();

    @Parameter(names = { "-siids", "--serviceInstanceIDs" }, variableArity = true)
    private List<String> _siids = new ArrayList<>();

    @Parameter(names = { "-ptype", "--respondToPayloadType" })
    private String       _payloadType;

    private String getPayloadType() {
      if (_payloadType == null) {
        _payloadType = ProbeWrapper.XML;
      }

      return _payloadType;
    }

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      try {
        Probe probe = new Probe(getPayloadType());

        if (_clientID != null) {
          probe.setClientID(_clientID);
        } else {
          probe.setClientID(context.getDefaultCID());
        }

        for (String scid : _scids) {
          probe.addServiceContractID(scid);
        }

        for (String siid : _siids) {
          probe.addServiceInstanceID(siid);
        }

        String name = (_probeName == null) ? "UNNAMED" : _probeName;

        // This really sucks - fix it - but how?
        if (context.getProbes().containsKey(name))
          name = name + "(1)";

        context.getProbes().put(name, probe);

        Console.info("Created new probe named [" + name + "]");

      } catch (UnsupportedPayloadType e) {
        e.printStackTrace();
        return CommandResult.BAD_ARGS;
      }

      return CommandResult.OK;
    }

  }

  /**
   * The DeleteProbe command deletes an existing probe.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "del" }, commandDescription = "deletes an existing probe.  It takes a list of probes names to delete.")
  public class DeleteProbe extends Command<ArgoClientContext> {

    @Parameter
    private List<String> _probeNames = new ArrayList<String>();

    @Parameter(names = { "-a", "--all" }, description = "delete all the probes.")
    private boolean      _deleteAll;

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      if (_deleteAll) {
        _probeNames = new ArrayList<String>(); // clear whatever was set on the
                                               // command line
        _probeNames.addAll(context.getProbes().keySet());
      }

      for (String probeName : _probeNames) {
        Probe probe = context.getProbe(probeName);
        if (probe == null) {
          Console.error("No exising probe named [" + probeName + "]");
        } else {
          context.getProbes().remove(probeName);
          Console.info("Deleted probe named [" + probeName + "]");
        }
      }
      return CommandResult.OK;
    }

  }

  /**
   * Modify an existing probe.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "mod" }, commandDescription = "modify an existing probe")
  public class ModifyProbe extends Command<ArgoClientContext> {

    @Parameter(names = { "-n", "--name" }, description = "name of the probe.", required = true)
    private String      _probeName;

    @Parameter(names = { "-cid", "--clientID" }, description = "cleint ID for the probe")
    private String      _clientID;

    @Parameter(names = { "-scids", "--serviceContractIDs" }, variableArity = true)
    public List<String> _scids = new ArrayList<>();

    @Parameter(names = { "-siids", "--serviceInstanceIDs" }, variableArity = true)
    public List<String> _siids = new ArrayList<>();

    @Parameter(names = { "-ptype", "--respondToPayloadType" })
    private String      _payloadType;

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      Probe probe = context.getProbe(_probeName);
      if (probe == null) {
        Console.error("No exising probe named [" + _probeName + "]");
        return CommandResult.ERROR;
      }

      if (_payloadType != null) {
        try {
          probe.setRespondToPayloadType(_payloadType);
        } catch (UnsupportedPayloadType e) {
          Console.error("Unsupported payload type [" + _payloadType + "]");
          return CommandResult.BAD_ARGS;
        }
      }

      if (_clientID != null) {
        probe.setClientID(_clientID);
      }

      if (!_scids.isEmpty()) {
        for (String serviceContractID : _scids) {
          probe.addServiceContractID(serviceContractID);
        }
      }

      if (!_siids.isEmpty()) {
        for (String serviceInstanceID : _scids) {
          probe.addServiceInstanceID(serviceInstanceID);
        }
      }

      return CommandResult.OK;
    }

  }

  /**
   * The ListProbes command will list the configured probes.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "list" }, commandDescription = "list the configured probes")
  public class ListProbes extends Command<ArgoClientContext> {

    @Parameter
    private List<String> _namePatterns = new ArrayList<String>();

    @Parameter(names = { "-p", "--payload" }, description = "show the actual probe payload")
    private boolean      _showPayload;

    @Parameter(names = { "-n", "--namesOnly" }, description = "just list the names")
    private boolean      _namesOnly;

    @Parameter(names = "-defaultCID", description = "show the current default CID")
    private boolean      _showDefaultCID;

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {
      HashMap<String, Probe> probes = context.getProbes();
      List<String> probeNames = new ArrayList<String>();

      if (_showDefaultCID)
        Console.info("Default CID: " + context.getDefaultCID());

      if (probes.isEmpty()) {
        Console.info("No current probes configured.");
      } else {
        if (_namePatterns.isEmpty()) {
          Console.info("Listing all configured probes.");
          probeNames.addAll(probes.keySet());
        } else {
          Console.info("Listing configured probes with names matching [" + _namePatterns + "]");
          for (String pattern : _namePatterns) {
            for (String probeName : probes.keySet()) {
              if (probeName.matches(pattern))
                probeNames.add(probeName);
            }
          }
        }

        Console.info("Configured probes:");
        for (String probeName : probeNames) {
          Probe probe = probes.get(probeName);

          if (_namesOnly) {
            Console.info("Probe: NAME [" + probeName + "]");
          } else {
            String probeInfoLine = probeInfoLine(probe, _showPayload);
            Console.info("Probe: NAME [" + probeName + "] | " + probeInfoLine);
          }

        }

      }
      return CommandResult.OK;
    }
  }

  /**
   * The ImportProbes command will import probes from a file.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "import" }, commandDescription = "import the specified probes")
  public class ImportProbes extends Command<ArgoClientContext> {

    @Parameter(names = { "-f", "--file" }, description = "the filename for import", required = true)
    private String _importFilename;

    @Override
    protected CommandResult innerExecute(ArgoClientContext context) {

      File fXmlFile = new File(_importFilename);

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder;
      try {
        dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);

        NodeList wList = doc.getElementsByTagName("clientProbeWrapper");
        for (int widx = 0; widx < wList.getLength(); widx++) {
          Element wrapperElem = (Element) wList.item(widx);

          Element probeElem = (Element) wrapperElem.getElementsByTagName("probe").item(0);

          // String des = probeElem.getAttribute("DESVersion");
          // String id = probeElem.getAttribute("id");

          String client = probeElem.getAttribute("client");

          String payloadType = probeElem.getElementsByTagName("respondToPayloadType").item(0).getTextContent();

          Probe probe = new Probe(payloadType);

          probe.setClientID(client);

          String hopLimit = wrapperElem.getAttribute("hopLimit");
          probe.setHopLimit(Integer.parseInt(hopLimit));

          // Get the RA list
          NodeList raList = ((Element) probeElem.getElementsByTagName("ra").item(0)).getElementsByTagName("respondTo");
          for (int raidx = 0; raidx < raList.getLength(); raidx++) {
            Element raElem = (Element) raList.item(raidx);
            String label = raElem.getAttribute("label");
            String ra = raElem.getTextContent();
            probe.addRespondToURL(label, ra);
          }

          // Get the SCID list
          if (probeElem.getElementsByTagName("scids").getLength() > 0) {
            NodeList scidList = ((Element) probeElem.getElementsByTagName("scids").item(0)).getElementsByTagName("scid");
            for (int scidx = 0; scidx < scidList.getLength(); scidx++) {
              Element scidElem = (Element) scidList.item(scidx);
              String scid = scidElem.getTextContent();
              probe.addServiceContractID(scid);
            }
          }

          // Get the SIID list
          if (probeElem.getElementsByTagName("siids").getLength() > 0) {
            NodeList siidList = ((Element) probeElem.getElementsByTagName("siids").item(0)).getElementsByTagName("siid");
            for (int siidx = 0; siidx < siidList.getLength(); siidx++) {
              Element siidElem = (Element) siidList.item(siidx);
              String siid = siidElem.getTextContent();
              probe.addServiceInstanceID(siid);
            }
          }

          String name = wrapperElem.getAttribute("name");
          context.getProbes().put(name, probe);

        }

        Console.info("Imported [" + wList.getLength() + "] from [" + _importFilename + "]");
      } catch (ParserConfigurationException | SAXException | IOException | UnsupportedPayloadType e) {
        Console.error("There was some issues parsing the import xml file [" + e.getMessage() + "]");
        // e.printStackTrace();
        return CommandResult.ERROR;
      }

      return CommandResult.OK;
    }

  }

  /**
   * The ExportProbes command will export the specified probes to a file.
   * 
   * @author jmsimpson
   *
   */
  @Parameters(commandNames = { "export" }, commandDescription = "Export the specified probes")
  public class ExportProbes extends Command<ArgoClientContext> {

    @Parameter(names = { "-f", "--file" }, description = "the filename of the export", required = true)
    private String       _exportFilename;

    @Parameter
    private List<String> _namePatterns = new ArrayList<String>();


    protected CommandResult innerExecute(ArgoClientContext context) {
      HashMap<String, Probe> probes = context.getProbes();
      List<String> probeNames = new ArrayList<String>();

      Console.info("Export Filename = " + _exportFilename);

      if (probes.isEmpty()) {
        Console.info("No current probes configured.");
      } else {
        if (_namePatterns.isEmpty()) {
          Console.info("Exporting all configured probes.");
          probeNames.addAll(probes.keySet());
        } else {
          Console.info("Exporting configured probes with names matching: " + _namePatterns);
          for (String pattern : _namePatterns) {
            for (String probeName : probes.keySet()) {
              if (probeName.matches(pattern))
                probeNames.add(probeName);
            }
          }
        }

        String xml = createExportXML(context, probeNames);

        writeXMLToFile(_exportFilename, xml);

        Console.info(xml);

      }
      return CommandResult.OK;
    }

    private void writeXMLToFile(String filename, String xml) {

      try (FileWriter writer = new FileWriter(filename)) {
        writer.write(xml);
      } catch (IOException e) {
        Console.error("Error writing file " + filename + " - " + e.getMessage());
      }
    }

    private String createExportXML(ArgoClientContext context, List<String> probeNames) {
      StringBuffer buf = new StringBuffer();

      buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      buf.append("<probes>\n");
      for (String probeName : probeNames) {
        Probe probe = context.getProbe(probeName);
        try {
          buf.append("<clientProbeWrapper ").append(" name=\"").append(probeName).append("\" ").append(" hopLimit=\"").append(probe.getHopLimit()).append("\"").append(">\n");
          buf.append(probe.asXMLFragment()).append("\n");
          buf.append("</clientProbeWrapper>\n");
        } catch (JAXBException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }
      buf.append("</probes>\n");

      return buf.toString();
    }
  }

  protected String probeInfoLine(Probe probe, boolean payload) {
    StringBuffer buf = new StringBuffer();
    if (payload) {
      try {
        buf.append("XML PAYLOAD ------ \n").append(probe.asXMLFragment());
      } catch (JAXBException e) {
        buf.append("Error marshalling probe XML. " + e.getMessage());
      }
    } else {
      buf.append(probe.asString());
    }
    return buf.toString();
  }

}
