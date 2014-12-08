package org.checkerframework.mavenplugin;

import org.apache.maven.plugin.CompilationFailureException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A CommandLineExecutor that reports the output and error streams with no additional processing.
 * @see org.checkerframework.mavenplugin.CommandLineExceutor
 */
public class JavacIOExecutor implements CommandLineExceutor {
    private final String pathToExecutable;

    public JavacIOExecutor(final String pathToExecutable) {
        this.pathToExecutable = pathToExecutable;
    }

    /**
     * {@inheritDoc}
     */
    public void executeCommandLine(final Commandline cl, final Log log, final boolean failOnError)
            throws MojoExecutionException, MojoFailureException {
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        log.debug("command line: " + Arrays.toString(cl.getCommandline()));

        // Executing the command
        final int exitCode;
        try {
            exitCode = CommandLineUtils.executeCommandLine(cl, out, out);
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to execute the Checker Framework, executable: " + pathToExecutable +
                    ", command line: " + Arrays.toString(cl.getCommandline()), e);
        }

        final String javacOutput = out.getOutput();

        // Sanity check - if the exit code is non-zero, there should be some messages
        if (exitCode != 0 && javacOutput.isEmpty()) {
            throw new MojoExecutionException("Exit code from the compiler was not zero (" + exitCode +
                    "), but no output was reported");
        }

        if (exitCode == 0) {
            log.info(javacOutput);
        } else {
            if (failOnError) {
                log.error(javacOutput);
                throw new MojoFailureException(null, "Errors found by the Checker(s)", javacOutput);
            } else {
                log.warn("Errors found by the Checker(s)");
                log.warn(javacOutput);
            }
        }
    }

}
