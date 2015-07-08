package net.dharwin.common.tools.cli.api;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.dharwin.common.tools.cli.api.annotations.CLIEntry;
import net.dharwin.common.tools.cli.api.console.Console;
import net.dharwin.common.tools.cli.api.exceptions.CLIInitException;
import net.dharwin.common.tools.cli.api.utils.CLIAnnotationDiscovereryListener;


import com.impetus.annovention.ClasspathDiscoverer;
import com.impetus.annovention.Discoverer;

/**
 * The entry point contains the main entry. It is in charge of
 * locating the desired CommandLineApplication implementation, as well
 * as initializing and starting it.
 * @author Sean
 *
 */
public class EntryPoint {
	
	public static void main(String[] args) {
		
		String cliEntryClassName = discoverCLIEntryClass();
		startClient(args, cliEntryClassName);
    
	}

  private static void startClient(String[] args, String cliEntryClassName) {
    Console.superFine("Loading CLIEntry ["+cliEntryClassName+"].");
		
    CommandLineApplication<? extends CLIContext> cla = null;
    try {
      cla = instantiateClient(cliEntryClassName);
    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | CLIInitException e1) {
      Console.severe("Unable to instantiate ["+cliEntryClassName+"]. "+e1.getLocalizedMessage());
      e1.printStackTrace();
      System.exit(1);
    }

    cla.start(args);
  }

  @SuppressWarnings("unchecked")
  private static CommandLineApplication<? extends CLIContext> instantiateClient(String cliEntryClassName) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CLIInitException {
    Class<?> cliEntryClass = Class.forName(cliEntryClassName);
    if (!CommandLineApplication.class.isAssignableFrom(cliEntryClass)) {
    	Console.severe("CLIEntry ["+cliEntryClassName+"] is not of type CommandLineApplication.");
    	throw new CLIInitException("CLIEntry ["+cliEntryClassName+"] is not of type CommandLineApplication.");
    }
    
    Constructor<?> constructor = cliEntryClass.getConstructor();
    
    CommandLineApplication<? extends CLIContext> cla = (CommandLineApplication<? extends CLIContext>)constructor.newInstance();
    return cla;
  }

  private static String discoverCLIEntryClass() {
    Discoverer discoverer = new ClasspathDiscoverer();
		CLIAnnotationDiscovereryListener discoveryListener = new CLIAnnotationDiscovereryListener(new String[] {CLIEntry.class.getName()});
		discoverer.addAnnotationListener(discoveryListener);
		discoverer.discover(true, true, true, true, true);

		if (discoveryListener.getDiscoveredClasses().isEmpty()) {
			Console.severe("Startup failed: Could not find CLIEntry.");
			System.exit(1);
		}
		
		String cliEntryClassName = discoveryListener.getDiscoveredClasses().get(0);
    return cliEntryClassName;
  }
}
