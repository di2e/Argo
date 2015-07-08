package net.dharwin.common.tools.cli.api;

/**
 * Arbitrary stuff.
 * @author Sean
 *
 */
public class StringUtils {
	
	/**
	 * Strip the first N items from the array.
	 * @param originalArgs The original array.
	 * @param stripCount How many items to strip.
	 * @return A copy of the original array with the first N items stripped off.
	 */
	public static String[] stripArgs(String[] originalArgs, int stripCount) {
		if (originalArgs.length <= stripCount) {
			return new String[0];
		}
		String[] stripped = new String[originalArgs.length - stripCount];
		for (int i = 0; i < stripped.length; i++) {
			stripped[i] = originalArgs[i+stripCount];
		}
		return stripped;
	}
	
}
