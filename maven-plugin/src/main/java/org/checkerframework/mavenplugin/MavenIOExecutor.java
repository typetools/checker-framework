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
 * A CommandLineExecutor that formats warning and error messages in a similar style to the maven-compiler-plugin.
 * @see org.checkerframework.mavenplugin.CommandLineExceutor
 */
public class MavenIOExecutor implements CommandLineExceutor {
    private final String pathToExecutable;

    public MavenIOExecutor(final String pathToExecutable) {
        this.pathToExecutable = pathToExecutable;
    }

    /**
     * {@inheritDoc}
     */
    public void executeCommandLine(final Commandline cl, final Log log, final boolean failOnError) throws MojoExecutionException, MojoFailureException {
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

        log.debug("command line: " + Arrays.toString(cl.getCommandline()));

        // Executing the command
        final int exitCode;
        try {
            exitCode = CommandLineUtils.executeCommandLine(cl, out, err);
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to execute the Checker Framework, executable: " + pathToExecutable +
                    ", command line: " + Arrays.toString(cl.getCommandline()), e);
        }

        // Parsing the messages from the compiler
        final List<CompilerError> messages;
        try {
            messages = JavacErrorMessagesParser.parseMessages(err.getOutput());
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Unable to parse messages.", e);
        }

        // Sanity check - if the exit code is non-zero, there should be some messages
        if (exitCode != 0 && messages.isEmpty()) {
            throw new MojoExecutionException("Exit code from the compiler was not zero (" + exitCode +
                    "), but no messages reported. Error stream content: " + err.getOutput() +
                    " command line: " + Arrays.toString(cl.getCommandline()));
        }

        if (messages.isEmpty()) {
            log.info("No errors found by the processor(s).");
        } else {
            if (failOnError) {
                final List<CompilerError> warnings = new ArrayList<CompilerError>();
                final List<CompilerError> errors   = new ArrayList<CompilerError>();
                for (final CompilerError message : messages) {
                    if (message.isError()) {
                        errors.add(message);
                    } else {
                        warnings.add(message);
                    }
                }

                if (!warnings.isEmpty()) {
                    logErrors(warnings, "warning", true, log);
                }

                logErrors(errors, "error", false, log);

                throw new MojoFailureException(null, "Errors found by the processor(s)", CompilationFailureException.longMessage(errors));

            } else {
                log.info("Run with debug logging in order to view the compiler command line");
                for (final CompilerError compilerError : messages) {
                    log.warn(compilerError.toString());
                }
            }
        }
    }

    /**
     * Print a header with the given label and print all errors similar to the style
     * maven itself uses
     * @param errors Errors to print
     * @param label The label (usually ERRORS or WARNINGS) to head the error list
     * @param log The log to which the messages are printed
     */
    private static final void logErrors(final List<CompilerError> errors, final String label,
                                        boolean warn, final Log log) {
        log.info("-------------------------------------------------------------");
        log.warn("CHECKER FRAMEWORK " + label.toUpperCase() + ": ");
        log.info("-------------------------------------------------------------");
        for (final CompilerError error : errors) {
            final String msg = error.toString().trim();
            if(warn) {
                log.warn(msg);
            } else {
                log.error(msg);
            }
        }

        final String labelLc = label.toLowerCase() + ((errors.size() == 1) ? "" : "s");
        log.info(errors.size() + " " + labelLc);
        log.info("-------------------------------------------------------------");
    }

}
