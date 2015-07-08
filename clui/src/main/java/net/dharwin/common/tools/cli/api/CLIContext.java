package net.dharwin.common.tools.cli.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import net.dharwin.common.tools.cli.api.console.Console;


/**
 * The CLIContext is a key/value datastore that gets passed between commands.
 * It should be used to persist state between command execution. Applications can
 * specify their own CLIContext, allowing for known keys to be used through getters/setters,
 * or to store/do anything else that might be needed.
 * @author Sean
 *
 */
public class CLIContext {
	
	/**
	 * The properties held by this context.
	 */
	private Map<String, Object> _properties;
	
	/**
	 * The application in which this context is contained.
	 */
	private CommandLineApplication<? extends CLIContext> _app;
	
	public CLIContext(CommandLineApplication<? extends CLIContext> app) {
		_properties = new HashMap<String, Object>();
		_app = app;
		loadProperties(getEmbeddedPropertiesFilename());
		loadProperties(getExternalPropertiesFile());
	}
	
	/**
	 * Get the application in which this context is contained.
	 * @return The application in which this context is contained.
	 */
	public CommandLineApplication<? extends CLIContext> getHostApplication() {
		return _app;
	}
	
	/**
	 * Load properties from a resource stream.
	 * @param propFileName The resource name.
	 */
	private void loadProperties(String propFileName) {
		if (propFileName != null) {
			loadProperties(getClass().getResourceAsStream(propFileName), propFileName);
		}
	}
	
	/**
	 * Load properties from a file.
	 * @param propFile
	 */
	private void loadProperties(File propFile) {
		if (propFile == null) {
			return;
		}
		try {
			loadProperties(new FileInputStream(propFile), propFile.getAbsolutePath());
		}
		catch (FileNotFoundException e) {
			Console.warn("Unable to find properties file ["+propFile.getAbsolutePath()+"].");
		}
	}
	
	/**
	 * Loads properties from the given stream.
	 * This will close the stream.
	 * @param stream The stream to load from.
	 * @param path The path represented by the stream.
	 */
	private void loadProperties(InputStream stream, String path) {
		if (stream == null) {
			return;
		}
		
		try {
			Properties props = new Properties();
			props.load(stream);
			
			Iterator<Object> keyIt = props.keySet().iterator();
			while (keyIt.hasNext()) {
				String key = keyIt.next().toString();
				_properties.put(key, props.get(key));
			}
		}
		catch (Exception e) {
			Console.warn("Unable to load properties file ["+path+"].");
		}
		finally {
			if (stream != null) {
				try {
					stream.close();
				}
				catch (Exception e) {
					Console.warn("Unable to close properties file ["+path+"].");
				}
			}
		}
	}
	
	/**
	 * Get the embedded property file. If none should be used, specify null.
	 * @return
	 */
	protected String getEmbeddedPropertiesFilename() {
		return null;
	}
	
	/**
	 * Get the external property file. If none should be used, specify null.
	 * @return
	 */
	protected File getExternalPropertiesFile() {
		return null;
	}
	
	/**
	 * Add an object to the context.
	 * @param key The key to add.
	 * @param o The object to add.
	 * @return The previous object associated with this key, or null if there was none.
	 */
	public Object put(String key, Object o) {
		return _properties.put(key, o);
	}
	
	public Object getObject(String key) {
		return _properties.get(key);
	}
	
	/**
	 * Get the string value, or null if not found.
	 * @param key The key to search for.
	 * @return The value, or null if not found.
	 */
	public String getString(String key) {
		return getString(key, null);
	}
	
	/**
	 * Get the string value, or the defaultValue if not found.
	 * @param key The key to search for.
	 * @return The value, or defaultValue if not found.
	 */
	public String getString(String key, String defaultValue) {
		Object o = getObject(key);
		if (o == null) {
			return defaultValue;
		}
		if (!(o instanceof String)) {
			throw new IllegalArgumentException("Object ["+o+"] associated with key ["+key+"] is not of type String.");
		}
		return (String)o;
	}
	
	/**
	 * Get the boolean value, or false if not found.
	 * @param key The key to search for.
	 * @return The value, or false if not found.
	 */
	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}
	
	/**
	 * Get the boolean value, or the defaultValue if not found.
	 * @param key The key to search for.
	 * @return The value, or defaultValue if not found.
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		Object o = getObject(key);
		if (o == null) {
			return defaultValue;
		}
		boolean b = defaultValue;
		try {
			b = Boolean.parseBoolean(o.toString());
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Object ["+o+"] associated with key ["+key+"] is not of type Boolean.");
		}
		return b;
	}
	
	/**
	 * Get the integer value, or -1 if not found.
	 * @param key The key to search for.
	 * @return The value, or -1 if not found.
	 */
	public int getInteger(String key) {
		return getInteger(key, -1);
	}
	
	/**
	 * Get the integer value, or the defaultValue if not found.
	 * @param key The key to search for.
	 * @return The value, or defaultValue if not found.
	 */
	public int getInteger(String key, int defaultValue) {
		Object o = getObject(key);
		if (o == null) {
			return defaultValue;
		}
		int val = defaultValue;
		try {
			val = Integer.parseInt(o.toString());
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Object ["+o+"] associated with key ["+key+"] is not of type Integer.");
		}
		return val;
	}
	
	/**
	 * Determine whether the given key exists in the context or not.
	 * @param key The key to search for.
	 * @return True if the key is contained within this context, false otherwise.
	 */
	public boolean containsKey(String key) {
		return _properties.containsKey(key);
	}
	
}
