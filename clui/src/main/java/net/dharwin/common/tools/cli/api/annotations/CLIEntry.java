package net.dharwin.common.tools.cli.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CLIEntry designates the entry point for a CLI application.
 * Applications implements the CLI API should define exactly one class
 * that contains this annotation. The designated class will be loaded and executed
 * upon startup.
 * @author Sean
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CLIEntry {
}
