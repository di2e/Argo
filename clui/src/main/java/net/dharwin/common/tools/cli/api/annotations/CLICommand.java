package net.dharwin.common.tools.cli.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any class implementing the Command interface should apply this annotation.
 * Using this annotation marks it for use by the CLI API, meaning it is
 * a candidate for command execution.
 * @author Sean
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CLICommand {
	
	/**
	 * Get the name of the command. This name is used for case-insensitive matching
	 * with the user's input.
	 * @return The command name.
	 */
	String name();
	
	/**
	 * Get the description of the command. Optional.
	 * @return The command description.
	 */
	String description() default "";
}
