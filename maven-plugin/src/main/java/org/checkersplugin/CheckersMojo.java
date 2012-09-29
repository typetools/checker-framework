package org.checkersplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.CompilationFailureException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.compiler.CompilerError;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.File;

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
	 * @parameter default-value="1.0.6"
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
	 * DEPENDENCIES
	 */
	
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
		
		final String checkersJar = PathUtils.getCheckersJar(checkersVersion, artifactFactory, artifactResolver,
				remoteArtifactRepositories, localRepository);

		final Commandline cl = new Commandline();

		if (StringUtils.isEmpty(executable)) {
			executable = "java";
		}
		cl.setExecutable(PathUtils.getExecutablePath(executable, toolchainManager, session));

		// Building the arguments
		final List<String> arguments = new ArrayList<String>();

		// Setting the boot class path: prepending the jar with jsr308 compiler
		arguments.add("-Xbootclasspath/p:" + checkersJar);
		// Javac currently assumes that assertions are enabled in the launcher
		arguments.add("-ea:com.sun.tools");
		// Optionally adding user-specified java parameters
		if (!StringUtils.isEmpty(javaParams)) {
			arguments.addAll(Arrays.asList(javaParams.split(" ")));
		}
		// Running the compile process - main class of this jar 
		arguments.add("-jar");
		arguments.add(checkersJar);

		// Now the arguments for the jar main class - that is, the compiler

		// Setting the name of the processor
		arguments.add("-processor");
		arguments.add(processor);
		// Running only the annotation processor, without compiling
		arguments.add("-proc:only");
		// Setting the classpath
		arguments.add("-classpath" );
		arguments.add(StringUtils.join(classpathElements.iterator(), File.pathSeparator));
		// Setting the source dir path
		arguments.add("-sourcepath");
		arguments.add(StringUtils.join(compileSourceRoots.iterator(), File.pathSeparator));
		// Optionally adding user-specified javac parameters
		if (!StringUtils.isEmpty(javacParams)) {
			arguments.addAll(Arrays.asList(javacParams.split(" ")));
		}
		// Now the source files
		arguments.addAll(sources);

		// And executing
		cl.addArguments(arguments.toArray(new String[arguments.size()]));

		executeCommandLine(cl, log);
	}

	private void executeCommandLine(final Commandline cl, final Log log) throws MojoExecutionException, MojoFailureException {
		CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
		CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
		
		log.debug("command line: " + Arrays.toString(cl.getCommandline()));
	
		
		// Executing the command
		int exitCode;
		try {
			exitCode = CommandLineUtils.executeCommandLine(cl, out, err);
		} catch (CommandLineException e) {
			throw new MojoExecutionException("Unable to execute checkers, executable: " + executable +
					", command line: " + Arrays.toString(cl.getCommandline()), e);
		}

		// Parsing the messages from the compiler
		List<CompilerError> messages;
		try {
			messages = JavacCompilerUtil.parseModernStream( new BufferedReader( new StringReader( err.getOutput() ) ) );
		} catch (IOException e) {
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
				throw new MojoFailureException(CompilationFailureException.longMessage(messages));
			} else {
                log.info("Run with debug logging in order to view the compiler command line");
				for (CompilerError compilerError : messages) {
					log.warn(compilerError.toString());
				}
			}
		}
	}
}
