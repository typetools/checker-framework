package org.checkersplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.CompilationFailureException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
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
 * Says "Hi" to the user.
 * @goal check
 */
public class CheckersMojo extends AbstractMojo {
	/**
	 * PARAMETERS
	 */

	/**
	 * @parameter
	 * @required
	 */
	private List<String> processors = new ArrayList<String>();

	/**
	 * A list of inclusion filters for the compiler.
	 *
	 * @parameter
	 */
	private Set<String> includes = new HashSet<String>();

	/**
	 * A list of exclusion filters for the compiler.
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
	 * @parameter
	 */
	private String executable;

	/**
	 * @parameter default-value="1.0.6"
	 */
	private String checkersVersion;

	/**
	 * @parameter
	 */
	private String javaParams;

	/**
	 * @parameter
	 */
	private String javacParams;

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

//	/**
//	 * @parameter expression="${project}"
//	 * @readonly
//	 * @required
//	 */
//	private MavenProject mavenProject;
//
//	/**
//	 * @parameter expression="${plugin.artifacts}"
//	 * @readonly
//	 * @required
//	 */
//	private List pluginArtifacts;

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

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Running JSR308 checkers version: " + checkersVersion);

		if (processors.size() == 0) {
			throw new MojoExecutionException("At least one checker must be specified!");
		}

		String processor = StringUtils.join(processors.iterator(), ",");

		getLog().info("Running processor(s): " + processor);

		List<String> sources = PathUtils.scanForSources(compileSourceRoots, includes, excludes);

		String checkersJar = PathUtils.getCheckersJar(checkersVersion, artifactFactory, artifactResolver,
				remoteArtifactRepositories, localRepository);

		Commandline cl = new Commandline();

		if (StringUtils.isEmpty(executable)) {
			executable = "java";
		}
		cl.setExecutable(PathUtils.getExecutablePath(executable, toolchainManager, session));

		// Building the arguments
		List<String> arguments = new ArrayList<String>();

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

		executeCommandLine(cl);
	}

	private void executeCommandLine(Commandline cl) throws MojoExecutionException, MojoFailureException {
		CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
		CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

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
					"), but no messages reported. Error stream content: " + err.getOutput());
		}

		if (messages.isEmpty()) {
			getLog().info("No errors found by the processor(s).");
		} else {
			if (failOnError) {
				throw new MojoFailureException(CompilationFailureException.longMessage(messages));
			} else {
				for (CompilerError compilerError : messages) {
					getLog().warn(compilerError.toString());
				}
			}
		}
	}
}
