package net.dharwin.common.tools.cli.api;

/**
 * An interface for parsing an input line into an array of String tokens.
 * @author Sean
 *
 */
public interface CommandLineParser {
	
	/**
	 * Parse a string into a string array.
	 * @param line The input line.
	 * @return The string array. May not be null.
	 */
	public String[] parse(String line);
}
