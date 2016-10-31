package org.checkerframework.mavenplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Implementations of CommandLineExecutor take a CommandLine object and a Log.  They then execute
 * the command line and log any relevant output.  The "failOnError" flag must be obeyed.
 * That is, if any checkers report errors and failOnError==true then the executeCommandLine should throw
 * a MojoFailureException in order to halt the Maven process.  Otherwise, the implementation should log
 * the errors and continue.
 */
public interface CommandLineExceutor {
    /**
     * Executes the given command line and writes any relevant errors, warnings, or messages to log.
     * @param cl CommandLine object to execute.
     * @param log Log to report to
     * @param failOnError If true, any errors found while executing cl will cause the Maven process to halt,
     *                    if false, errors will only be reported but the Maven process will continue
     * @throws MojoExecutionException Thrown if there is any exception in the attempt to run the command
     * @throws MojoFailureException Thrown if the executed CommandLine reports errors and failOnError = true
     */
    public void executeCommandLine(final Commandline cl, final Log log, final boolean failOnError)
            throws MojoExecutionException, MojoFailureException;
}
