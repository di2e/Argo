package ws.argo.CLClient;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * NOTE: This is an experimental class at the moment. Its purpose is to help
 * parse the actual command line arguments from the java command line.
 * 
 * @author jmsimpson
 *
 */
@Parameters(commandDescription = "command line arguments")
public class JavaCommandLineArguments {

  @Parameter(names = { "-ni", "--networkIntefaces" }, description = "list of network interfaces to send probes.")
  public List<String> _probeNames = new ArrayList<>();

}
