package org.checkersplugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.CompilationFailureException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * A Mojo is the main goal or task for a maven project.  CheckersMojo runs the CheckerFramework compiler with the
 * checkers specified in the plugin configuration in the pom.xml.
 *
 * Note: requiresDependencyResolution ensures that the dependencies required for compilation are included in the
 * classPathElements which gets passed to the classpath of the JSR308 compiler
 * @requiresDependencyResolution compile
 * @goal check
 */
public class CheckersMojo extends AbstractMojo {
    /**
     * PARAMETERS
     */

    /**
     * The list of checkers to pass to the checker compiler
     *
     * @parameter
     * @required
     */
    private List<String> processors = new ArrayList<String>();

    /**
     * A list of inclusion filters for the compiler.
     * When CheckersMojo scans the "${compileSourceRoot}" directory for files it will only include those files
     * that match one of the specified inclusion patterns.  If no patterns are included then
     * PathUtils.DEFAULT_INCLUSION_PATTERN is used
     *
     * @parameter
     */
    private Set<String> includes = new HashSet<String>();

    /**
     * A list of exclusion filters for the compiler.  When CheckersMojo scans the "${compileSourceRoot}"
     * directory for files it will only include those file that DO NOT match any of the
     * specified exclusion patterns.
     *
     * @parameter
     */
    private Set<String> excludes = new HashSet<String>();

    /**
     * Should the build fail on a checker compile error.
     *
     * @parameter default-value="true"
     */
    private boolean failOnError;

    /**
     * The path to the java executable to use, default is "java"
     * This executable is used to call the jsr308 compiler jar
     * @parameter
     */
    private String executable;

    /**
     * Which version of the JSR308 checkers to use
     * @parameter default-value="${plugin.version}"
     */
    private String checkersVersion;

    /**
     * Java runtime parameters added when running the JSR308 compiler jar
     * @parameter
     */
    private String javaParams;

    /**
     * Javac params passed to the JSR308 compiler jar
     * @parameter
     */
    private String javacParams;

    /**
     * Whether to skip execution
     * @parameter expression="${checkers.skip}" default-value="false"
     */
     private boolean skip;

    /**
     * Whether to do only checking, without any subsequent compilation
     * @parameter default-value="true"
     */
     private boolean procOnly;

    /**
     * DEPENDENCIES
     */

    /**
      * @parameter expression="${project.build.outputDirectory}"
      * @required
      * @readonly
      */
     private String outputDirectory;

    /**
     * The source directories containing the sources to be compiled.
     *
     * @parameter expression="${project.compileSourceRoots}"
     * @required
     * @readonly
     */
    private List<String> compileSourceRoots;

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * @component
     * @required
     * @readonly
     */
    private ToolchainManager toolchainManager;

    /**
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List<?> remoteArtifactRepositories;

    /**
     * @parameter expression="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    private List<?> classpathElements;


    /**
     * The location of the Framework jar
     */
    private File checkersJar;

    /**
     * The location of the Compiler Jar
     */
    private File javacJar;

    /**
     * The location of the jdk7 jar
     */
    private File jdk7Jar;

    /**
     * Main control method for the Checker Maven Plugin.  Scans for sources, resolves classpath, and passes these
     * arguments to the the checker compiler which is run on the command line.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = getLog();

        if (skip) {
            log.info("Execution is skipped");
            return;
        } else if ("pom".equals(project.getPackaging())) {
            log.info("Execution is skipped for project with packaging 'pom'");
            return;
        }

        log.info("Running JSR308 checkers version: " + checkersVersion);

        if (processors.size() == 0) {
            throw new MojoExecutionException("At least one checker must be specified!");
        }

        final String processor = StringUtils.join(processors.iterator(), ",");

        log.info("Running processor(s): " + processor);

        final List<String> sources = PathUtils.scanForSources(compileSourceRoots, includes, excludes);

        if (sources.size() == 0) {
            log.info("No source files found.");
            return;
        }

        locateArtifacts();

        final Commandline cl = new Commandline();

        if ( StringUtils.isEmpty(executable) ) {
            executable = "java";
        }

        final String executablePath = PathUtils.getExecutablePath(executable, toolchainManager, session);
        cl.setExecutable(executablePath);

        //TODO: SEEMS THAT WHEN WE ARE USING @ ARGS THE CLASSPATH FROM THE JAR IS OVERRIDDEN - FIX THIS
        final String classpath  = checkersJar.getAbsolutePath() + File.pathSeparator
        		+ StringUtils.join(classpathElements.iterator(), File.pathSeparator);

        File srcFofn = null;
        File cpFofn = null;
        try {
            srcFofn = PluginUtil.writeTmpSrcFofn("CFPlugin-maven-src", true, PluginUtil.toFiles(sources));
            cpFofn  = PluginUtil.writeTmpCpFile("CFPlugin-maven-cp",   true, classpath);
        } catch (IOException e) {
            if(srcFofn != null && srcFofn.exists()) {
                srcFofn.delete();
            }
            if(cpFofn  != null && cpFofn.exists()) {
                cpFofn.delete();
            }
            throw new MojoExecutionException("Exception trying to write command file fofn!", e);
        }


        final Map<PluginUtil.CheckerProp, Object> props = makeProps();

        final List<String> arguments = PluginUtil.getCmdArgsOnly(
                javacJar, jdk7Jar,
                srcFofn, processor, checkersJar.getAbsolutePath(),
                null, cpFofn, null, props, null,
                procOnly, outputDirectory);

        // And executing
        cl.addArguments(arguments.toArray(new String[arguments.size()]));

        executeCommandLine(cl, log);
        srcFofn.delete();
        cpFofn.delete();
    }

    /**
     * TODO: Think of a better way to do CheckerProps, it's weird to have some params built in
     * TODO: and some as MISC_OPTIONS
     * @return
     */
    private Map<PluginUtil.CheckerProp, Object> makeProps() {

        final String sourcePath = StringUtils.join(compileSourceRoots.iterator(), File.pathSeparator);

        final List<String> miscOptions = new ArrayList<String>();
        miscOptions.add("-sourcepath");
        miscOptions.add(sourcePath);

        // Optionally adding user-specified java parameters
        if (!StringUtils.isEmpty(javaParams)) {
            miscOptions.addAll(PluginUtil.toJavaOpts(Arrays.asList(javaParams.split(" "))));
        }

        // Optionally adding user-specified javac parameters
        if (!StringUtils.isEmpty(javacParams)) {
            miscOptions.addAll(Arrays.asList(javacParams.split(" ")));
        }

        final Map<PluginUtil.CheckerProp, Object> props = new HashMap<PluginUtil.CheckerProp, Object>();
        props.put(PluginUtil.CheckerProp.MISC_COMPILER, miscOptions);

        return props;
    }

    /**
     * Execute the given command line and log any errors and debug info
     * @param cl
     * @param log
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    private void executeCommandLine(final Commandline cl, final Log log) throws MojoExecutionException, MojoFailureException {
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

        log.debug("command line: " + Arrays.toString(cl.getCommandline()));

        // Executing the command
        final int exitCode;
        try {
            exitCode = CommandLineUtils.executeCommandLine(cl, out, err);
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to execute checkers, executable: " + executable +
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
        log.warn("CHECKERS " + label.toUpperCase() + ": ");
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

    /**
     * Find the location of all the necessary Checker Framework related artifacts.  If the
     * artifacts have not been downloaded, download them.  Then copy them to the checker-maven-plugin
     * directory (overwriting any other version that is there).
     * @throws MojoExecutionException
     */
    private final void locateArtifacts() throws MojoExecutionException {

        checkersJar = PathUtils.getFrameworkJar("framework", checkersVersion,
                artifactFactory, artifactResolver, remoteArtifactRepositories, localRepository);

        javacJar    = PathUtils.getFrameworkJar("compiler", checkersVersion,
                artifactFactory, artifactResolver, remoteArtifactRepositories, localRepository);

        jdk7Jar    = PathUtils.getFrameworkJar("jdk7", checkersVersion,
                artifactFactory, artifactResolver, remoteArtifactRepositories, localRepository);
    }
}
