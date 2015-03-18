package org.checkerframework.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.ToolchainManager;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.*;
import java.util.*;

/**
 * A Mojo is the main goal or task for a maven project.  CheckersMojo runs the Checker Framework with the
 * checkers specified in the plugin configuration in the pom.xml.
 *
 * Note: requiresDependencyResolution ensures that the dependencies required for compilation are included in the
 * classPathElements which gets passed to the classpath of the JSR 308 compiler
 * @requiresDependencyResolution compile
 * @goal check
 */
public class CheckersMojo extends AbstractMojo {
    /**
     * PARAMETERS
     */

    /**
     * The list of checkers for the Checker Framework to run
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
     * This executable is used to call the JSR 308 compiler jar
     * @parameter
     */
    private String executable;

    /**
     * Which version of the Checker Framework to use
     * @parameter default-value="${plugin.version}"
     */
    private String checkerFrameworkVersion;

    /**
     * Java runtime parameters added when running the JSR 308 compiler jar
     * @parameter
     */
    private String javaParams;

    /**
     * Javac params passed to the JSR 308 compiler jar
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
     * If true, the error reporting output will show the verbatim javac output
     * @parameter default-value="false"
     */
    private boolean useJavacOutput;

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
     * The location of the Checker Framework jar
     */
    private File checkerJar;


    /**
     * The location of the compiler jar
     */
    private File javacJar;

    /**
     * The location of the annotated jdk jar
     */
    private File jdkJar;

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

        log.info("Running Checker Framework version: " + checkerFrameworkVersion);

        final String processor = (processors.size() > 0) ? StringUtils.join(processors.iterator(), ",") : null;

        if (processors.size() == 0) {
            log.warn("No checkers have been specified.");
        } else {
            log.info("Running processor(s): " + processor);
        }

        final List<String> sources = PathUtils.scanForSources(compileSourceRoots, includes, excludes);

        if (sources.size() == 0) {
            log.info("No source files found.");
            return;
        }

        locateArtifacts();

        final Commandline cl = new Commandline();

        if (StringUtils.isEmpty(executable)) {
            executable = "java";
        }

        final String executablePath = PathUtils.getExecutablePath(executable, toolchainManager, session);
        cl.setExecutable(executablePath);

        //TODO: SEEMS THAT WHEN WE ARE USING @ ARGS THE CLASSPATH FROM THE JAR IS OVERRIDDEN - FIX THIS
        final String classpath =
                checkerJar.getAbsolutePath() + File.pathSeparator
                + StringUtils.join(classpathElements.iterator(), File.pathSeparator);

        File srcFofn = null;
        File cpFofn = null;
        try {
            srcFofn = PluginUtil.writeTmpSrcFofn("CFPlugin-maven-src", true, PluginUtil.toFiles(sources));
            cpFofn = PluginUtil.writeTmpCpFile("CFPlugin-maven-cp", true, classpath);
        } catch (IOException e) {
            if (srcFofn != null && srcFofn.exists()) {
                srcFofn.delete();
            }
            if (cpFofn != null && cpFofn.exists()) {
                cpFofn.delete();
            }
            throw new MojoExecutionException("Exception trying to write command file fofn!", e);
        }

        final File outputDirFile = new File(outputDirectory);
        if (!procOnly && !outputDirFile.exists()) {
            if (!outputDirFile.mkdirs()) {
                throw new MojoExecutionException("Could not create output directory: " + outputDirFile.getAbsolutePath());
            }
        }

        final Map<PluginUtil.CheckerProp, Object> props = makeProps();

        final List<String> arguments = PluginUtil.getCmdArgsOnly(
                javacJar, jdkJar,
                srcFofn, processor, checkerJar.getAbsolutePath(),
                null, cpFofn, null, props, null,
                procOnly, outputDirectory);

        // And executing
        cl.addArguments(arguments.toArray(new String[arguments.size()]));

        createCommandLineExecutor().executeCommandLine(cl, log, failOnError);
        srcFofn.delete();
        cpFofn.delete();
    }

    /**
     * TODO: Think of a better way to do CheckerProps, it's weird to have some params built in
     * TODO: and some as MISC_OPTIONS
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
     * Find the location of all the necessary Checker Framework related artifacts.  If the
     * artifacts have not been downloaded, download them.  Then copy them to the checker-maven-plugin
     * directory (overwriting any other version that is there).
     * @throws MojoExecutionException
     */
    private final void locateArtifacts() throws MojoExecutionException {
        checkerJar = PathUtils.getFrameworkJar("checker", checkerFrameworkVersion,
                artifactFactory, artifactResolver, remoteArtifactRepositories, localRepository);

        javacJar    = PathUtils.getFrameworkJar("compiler", checkerFrameworkVersion,
                artifactFactory, artifactResolver, remoteArtifactRepositories, localRepository);

        final String jdkVersionStr = PluginUtil.getJdkJarPrefix();
        jdkJar = PathUtils.getFrameworkJar(jdkVersionStr, checkerFrameworkVersion,
                artifactFactory, artifactResolver, remoteArtifactRepositories, localRepository);
    }

    public CommandLineExceutor createCommandLineExecutor() {
        if( useJavacOutput ) {
            return new JavacIOExecutor(executable);
        } else {
            return new MavenIOExecutor(executable);
        }
    }
}
