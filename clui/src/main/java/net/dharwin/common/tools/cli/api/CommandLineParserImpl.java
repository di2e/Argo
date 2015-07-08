package net.dharwin.common.tools.cli.api;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the input line. This allows for quoted strings to be parsed as
 * a single token.
 * @author Sean
 *
 */
public class CommandLineParserImpl implements CommandLineParser {
	
	private static final Pattern PARSE_PATTERN = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
	
	@Override
	public String[] parse(String line) {
		List<String> matchList = new ArrayList<String>();
		Matcher regexMatcher = PARSE_PATTERN.matcher(line);
		while (regexMatcher.find()) {
		    if (regexMatcher.group(1) != null) {
		        // Add double-quoted string without the quotes
		        matchList.add(regexMatcher.group(1));
		    } else if (regexMatcher.group(2) != null) {
		        // Add single-quoted string without the quotes
		        matchList.add(regexMatcher.group(2));
		    } else {
		        // Add unquoted word
		        matchList.add(regexMatcher.group());
		    }
		}
		return matchList.toArray(new String[0]);
	}

}
