package org.checkerframework.framework.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * This class behaves similarly to javac. CheckerMain does the following:
 *
 * <ul>
 *   <li>add the jsr308-langtools {@code javac.jar} to the runtime bootclasspath of the process that
 *       runs the Checker Framework. This specifies which classes are used to run javac
 *   <li>add {@code jdk7.jar} or {@code jdk8.jar} to the compile time bootclasspath of the javac
 *       argument list passed to javac
 *   <li>parse and implement any special options used by the Checker Framework, e.g., using
 *       "shortnames" for annotation processors
 *   <li>pass all remaining command-line arguments to the real javac
 * </ul>
 *
 * To debug this class, use the {@code -AoutputArgsToFile=FILENAME} command-line argument or {@code
 * -AoutputArgsToFile=-} to output to standard out.
 *
 * <p>"To run the Checker Framework" really means to run java, where the program being run is a
 * special version of javac, and javac is passed a {@code -processor} command-line argument that
 * mentions a Checker Framework checker. There are 5 relevant classpaths: The classpath and
 * bootclasspath when running java, and the classpath, bootclasspath, and processorpath used by
 * javac. The latter three are the only important ones.
 *
 * <p>Note for developers: Try to limit the work done (and options interpreted) by CheckerMain,
 * because its functionality is not available to users who choose not to use the Checker Framework
 * javac script.
 */
public class CheckerMain {

    /**
     * Invoke the JSR 308 Type Annotations Compiler. Any exception thrown by the Checker Framework
     * escapes to the command line.
     */
    public static void main(String[] args) {
        final File pathToThisJar = new File(findPathTo(CheckerMain.class, false));
        ArrayList<String> alargs = new ArrayList<>(args.length);
        alargs.addAll(Arrays.asList(args));
        final CheckerMain program = new CheckerMain(pathToThisJar, alargs);
        final int exitStatus = program.invokeCompiler();
        System.exit(exitStatus);
    }

    /** The path to the annotated jdk jar to use */
    protected final File jdkJar;

    /** The path to the jsr308 Langtools Type Annotations Compiler */
    protected final File javacJar;

    /** The path to the jar containing CheckerMain.class (i.e. checker.jar) */
    protected final File checkerJar;

    /** The path to checker-qual.jar */
    protected final File checkerQualJar;

    private final List<String> compilationBootclasspath;

    private final List<String> runtimeClasspath;

    private final List<String> jvmOpts;

    private final List<String> cpOpts;

    private final List<String> ppOpts;

    private final List<String> toolOpts;

    private final List<File> argListFiles;

    /**
     * Construct all the relevant file locations and Java version given the path to this jar and a
     * set of directories in which to search for jars.
     */
    public CheckerMain(final File checkerJar, final List<String> args) {

        this.checkerJar = checkerJar;
        final File searchPath = checkerJar.getParentFile();
        this.checkerQualJar = new File(searchPath, "checker-qual.jar");

        replaceShorthandProcessor(args);
        argListFiles = collectArgFiles(args);

        this.javacJar =
                extractFileArg(PluginUtil.JAVAC_PATH_OPT, new File(searchPath, "javac.jar"), args);

        final String jdkJarName = PluginUtil.getJdkJarName();
        this.jdkJar =
                extractFileArg(PluginUtil.JDK_PATH_OPT, new File(searchPath, jdkJarName), args);

        this.compilationBootclasspath = createCompilationBootclasspath(args);
        this.runtimeClasspath = createRuntimeClasspath(args);
        this.jvmOpts = extractJvmOpts(args);

        this.cpOpts = createCpOpts(args);
        this.ppOpts = createPpOpts(args);
        this.toolOpts = args;

        assertValidState();
    }

    protected void assertValidState() {
        assertFilesExist(Arrays.asList(javacJar, jdkJar, checkerJar, checkerQualJar));
    }

    public void addToClasspath(List<String> cpOpts) {
        this.cpOpts.addAll(cpOpts);
    }

    public void addToProcessorpath(List<String> ppOpts) {
        this.ppOpts.addAll(ppOpts);
    }

    public void addToRuntimeClasspath(List<String> runtimeClasspathOpts) {
        this.runtimeClasspath.addAll(runtimeClasspathOpts);
    }

    protected List<String> createRuntimeClasspath(final List<String> argsList) {
        return new ArrayList<String>(Arrays.asList(javacJar.getAbsolutePath()));
    }

    protected List<String> createCompilationBootclasspath(final List<String> argsList) {
        final List<String> extractedBcp = extractBootClassPath(argsList);
        extractedBcp.add(0, jdkJar.getAbsolutePath());

        return extractedBcp;
    }

    protected List<String> createCpOpts(final List<String> argsList) {
        final List<String> extractedOps = extractCpOpts(argsList);
        extractedOps.add(0, this.checkerQualJar.getAbsolutePath());
        return extractedOps;
    }

    // Assumes that createCpOpts has already been run.
    protected List<String> createPpOpts(final List<String> argsList) {
        final List<String> extractedOps = extractPpOpts(argsList);
        if (extractedOps.isEmpty()) {
            // If processorpath is not provided, then javac uses the classpath.
            // CheckerMain always supplies a processorpath, so if the user
            // didn't specify a processorpath, then use the classpath.
            extractedOps.addAll(this.cpOpts);
        }
        extractedOps.add(0, this.checkerJar.getAbsolutePath());
        return extractedOps;
    }

    /**
     * Return the arguments that start with @ and therefore are files that contain javac arguments.
     *
     * @param args a list of command-line arguments; is not modified
     * @return a List of files representing all arguments that started with @
     */
    protected List<File> collectArgFiles(final List<String> args) {
        final List<File> argListFiles = new ArrayList<File>();
        for (final String arg : args) {
            if (arg.startsWith("@")) {
                argListFiles.add(new File(arg.substring(1)));
            }
        }

        return argListFiles;
    }

    /**
     * Remove the argument given by argumentName and the subsequent value from the list args if
     * present. Return the subsequent value.
     *
     * @param argumentName a command-line option name whose argument to extract
     * @param alternative default value to return if argumentName does not appear in args
     * @param args the current list of arguments
     * @return the string that follows argumentName if argumentName is in args, or alternative if
     *     argumentName is not present in args
     */
    protected static String extractArg(
            final String argumentName, final String alternative, final List<String> args) {
        int i = args.indexOf(argumentName);
        if (i == -1) {
            return alternative;
        } else if (i == args.size() - 1) {
            throw new RuntimeException(
                    "Argument " + argumentName + " specified but given no value!");
        } else {
            args.remove(i);
            return args.remove(i);
        }
    }

    /**
     * Remove the argument given by argumentName and the subsequent value from the list args if
     * present. Return the subsequent value wrapped as a File.
     *
     * @param argumentName argument to extract
     * @param alternative file to return if argumentName is not found in args
     * @param args the current list of arguments
     * @return the string that follows argumentName wrapped as a File if argumentName is in args or
     *     alternative if argumentName is not present in args
     */
    protected static File extractFileArg(
            final String argumentName, final File alternative, final List<String> args) {
        final String filePath = extractArg(argumentName, null, args);
        if (filePath == null) {
            return alternative;
        } else {
            return new File(filePath);
        }
    }

    /**
     * Find all args that match the given pattern and extract their index 1 group. Add all the index
     * 1 groups to the returned list. Remove all matching args from the input args list.
     *
     * @param pattern a pattern with at least one matching group
     * @param allowEmpties whether or not to add empty group(1) matches to the returned list
     * @param args the arguments to extract from
     * @return a list of arguments from the first group that matched the pattern for each input args
     *     or the empty list if there were none
     */
    protected static List<String> extractOptWithPattern(
            final Pattern pattern, boolean allowEmpties, final List<String> args) {
        final List<String> matchedArgs = new ArrayList<String>();

        int i = 0;
        while (i < args.size()) {
            final Matcher matcher = pattern.matcher(args.get(i));
            if (matcher.matches()) {
                final String arg = matcher.group(1).trim();

                if (!arg.isEmpty() || allowEmpties) {
                    matchedArgs.add(arg);
                }

                args.remove(i);
            } else {
                i++;
            }
        }

        return matchedArgs;
    }

    /**
     * A pattern to match bootclasspath prepend entries, used to construct one {@code
     * -Xbootclasspath/p:} command-line argument
     */
    protected static final Pattern BOOT_CLASS_PATH_REGEX =
            Pattern.compile("^(?:-J)?-Xbootclasspath/p:(.*)$");

    // TODO: Why does this treat -J and -J-X the same?  They have different semantics, don't they?
    /**
     * Remove all {@code -Xbootclasspath/p:} or {@code -J-Xbootclasspath/p:} arguments from args and
     * add them to the returned list.
     *
     * @param args the arguments to extract from
     * @return all non-empty arguments matching BOOT_CLASS_PATH_REGEX or an empty list if there were
     *     none
     */
    protected static List<String> extractBootClassPath(final List<String> args) {
        return extractOptWithPattern(BOOT_CLASS_PATH_REGEX, false, args);
    }

    /** Matches all {@code -J} arguments */
    protected static final Pattern JVM_OPTS_REGEX = Pattern.compile("^(?:-J)(.*)$");

    /**
     * Remove all {@code -J} arguments from {@code args} and add them to the returned list (without
     * the {@code -J} prefix)
     *
     * @param args the arguments to extract from
     * @return all {@code -J} arguments (without the {@code -J} prefix) or an empty list if there
     *     were none
     */
    protected static List<String> extractJvmOpts(final List<String> args) {
        return extractOptWithPattern(JVM_OPTS_REGEX, false, args);
    }

    /**
     * Remove the {@code -cp} and {@code -classpath} options and their arguments from args. Return
     * the last argument. If no {@code -cp} or {@code -classpath} arguments were present then return
     * the CLASSPATH environment variable followed by the current directory.
     *
     * @param args a list of arguments to extract from
     * @return the arguments that should be put on the classpath when calling javac.jar
     */
    protected static List<String> extractCpOpts(final List<String> args) {
        List<String> actualArgs = new ArrayList<String>();

        String path = null;

        for (int i = 0; i < args.size(); i++) {
            if ((args.get(i).equals("-cp") || args.get(i).equals("-classpath"))
                    && (i + 1 < args.size())) {
                args.remove(i);
                path = args.remove(i);
                // re-process whatever is currently at element i
                i--;
            }
        }

        // The logic below is exactly what the javac script does.
        // If it's empty use the "CLASSPATH" environment variable followed by the current directory.
        if (path == null) {
            final String systemClassPath = System.getenv("CLASSPATH");
            if (systemClassPath != null && !systemClassPath.trim().isEmpty()) {
                actualArgs.add(System.getenv("CLASSPATH"));
            }

            actualArgs.add(".");
        } else {
            // Every classpath entry overrides the one before it and CLASSPATH.
            actualArgs.add(path);
        }

        return actualArgs;
    }

    /**
     * Remove the {@code -processorpath} options and their arguments from args. Return the last
     * argument.
     *
     * @param args a list of arguments to extract from
     * @return the arguments that should be put on the processorpath when calling javac.jar
     */
    protected static List<String> extractPpOpts(final List<String> args) {
        List<String> actualArgs = new ArrayList<String>();

        String path = null;

        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals("-processorpath") && (i + 1 < args.size())) {
                args.remove(i);
                path = args.remove(i);
                // re-process whatever is currently at element i
                i--;
            }
        }

        if (path != null) {
            actualArgs.add(path);
        }

        return actualArgs;
    }

    protected void addMainToArgs(final List<String> args) {
        args.add("com.sun.tools.javac.Main");
    }

    /**
     * Invoke the JSR308 Type Annotations Compiler with all relevant jars on its classpath or boot
     * classpath
     */
    public List<String> getExecArguments() {
        List<String> args =
                new ArrayList<String>(jvmOpts.size() + cpOpts.size() + toolOpts.size() + 7);

        final String java = PluginUtil.getJavaCommand(System.getProperty("java.home"), System.out);
        args.add(java);

        args.add("-classpath");
        args.add(PluginUtil.join(File.pathSeparator, runtimeClasspath));
        args.add("-ea");
        // com.sun.tools needs to be enabled separately
        args.add("-ea:com.sun.tools...");

        args.addAll(jvmOpts);

        addMainToArgs(args);

        // No classes on the compilation bootclasspath will be loaded
        // during compilation, but the classes are read by the compiler
        // without loading them.  The compiler assumes that any class on
        // this bootclasspath will be on the bootclasspath of the JVM used
        // to later run the classfiles that Javac produces.  Our
        // jdk[78].jar classes don't have bodies, so they won't be used at
        // run time, but other, real definitions of those classes will be
        // on the classpath at run time.
        args.add(
                "-Xbootclasspath/p:"
                        + PluginUtil.join(File.pathSeparator, compilationBootclasspath));

        if (!argsListHasClassPath(argListFiles)) {
            args.add("-classpath");
            args.add(quote(PluginUtil.join(File.pathSeparator, cpOpts)));
        }
        if (!argsListHasProcessorPath(argListFiles)) {
            args.add("-processorpath");
            args.add(quote(PluginUtil.join(File.pathSeparator, ppOpts)));
        }

        args.addAll(toolOpts);
        return args;
    }

    /**
     * Invoke the JSR308 Type Annotations Compiler with all relevant jars on its classpath or boot
     * classpath
     */
    public int invokeCompiler() {
        List<String> args = getExecArguments();

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);

            if (arg.startsWith("-AoutputArgsToFile=")) {
                String fileName = arg.substring(19);
                args.remove(i);
                outputArgumentsToFile(fileName, args);
                break;
            }
        }

        // Actually invoke the compiler
        return ExecUtil.execute(args.toArray(new String[args.size()]), System.out, System.err);
    }

    private static void outputArgumentsToFile(String outputFilename, List<String> args) {
        if (outputFilename != null) {
            String errorMessage = null;

            try {
                PrintWriter writer =
                        (outputFilename.equals("-")
                                ? new PrintWriter(System.out)
                                : new PrintWriter(outputFilename, "UTF-8"));
                for (int i = 0; i < args.size(); i++) {
                    String arg = args.get(i);

                    // We would like to include the filename of the argfile instead of its contents.
                    // The problem is that the file will sometimes disappear by the time the user can
                    // look at or run the resulting script. Maven deletes the argfile very shortly
                    // after it has been handed off to javac, for example. Ideally we would print
                    // the argfile filename as a comment but the resulting file couldn't then be run as
                    // a script on Unix or Windows.
                    if (arg.startsWith("@")) {
                        // Read argfile and include its parameters in the output file.
                        String inputFilename = arg.substring(1);

                        BufferedReader br = new BufferedReader(new FileReader(inputFilename));
                        String line;
                        while ((line = br.readLine()) != null) {
                            writer.print(line);
                            writer.print(" ");
                        }
                        br.close();
                    } else {
                        writer.print(arg);
                        writer.print(" ");
                    }
                }
                writer.close();
            } catch (IOException e) {
                errorMessage = e.toString();
            }

            if (errorMessage != null) {
                System.err.println(
                        "Failed to output command-line arguments to file "
                                + outputFilename
                                + " due to exception: "
                                + errorMessage);
            }
        }
    }

    /**
     * Returns true if some @arglist file sets the classpath.
     *
     * @param argListFiles command-line argument files (specified with @ on the command line)
     */
    private static boolean argsListHasClassPath(final List<File> argListFiles) {
        for (final String arg : expandArgFiles(argListFiles)) {
            if (arg.contains("-classpath") || arg.contains("-cp")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if some @arglist file sets the processorpath.
     *
     * @param argListFiles command-line argument files (specified with @ on the command line)
     */
    private static boolean argsListHasProcessorPath(final List<File> argListFiles) {
        for (final String arg : expandArgFiles(argListFiles)) {
            if (arg.contains("-processorpath")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return all the lines in all the files.
     *
     * @param files a list of files
     * @return a list of all the lines in all the files
     */
    protected static List<String> expandArgFiles(final List<File> files) {
        final List<String> content = new ArrayList<String>();
        for (final File file : files) {
            try {
                content.addAll(PluginUtil.readFile(file));
            } catch (final IOException exc) {
                throw new RuntimeException("Could not open file: " + file.getAbsolutePath(), exc);
            }
        }
        return content;
    }

    /**
     * Find the jar file or directory containing the .class file from which cls was loaded
     *
     * @param cls the class whose .class file we wish to locate; if null, CheckerMain.class.
     * @param errIfFromDirectory if false, throw an exception if the file was loaded from a
     *     directory
     */
    public static String findPathTo(Class<?> cls, boolean errIfFromDirectory)
            throws IllegalStateException {
        if (cls == null) {
            cls = CheckerMain.class;
        }
        String name = cls.getName();
        String classFileName;
        /* name is something like package.name.ContainingClass$ClassName. We need to turn this into ContainingClass$ClassName.class. */
        {
            int idx = name.lastIndexOf('.');
            classFileName = (idx == -1 ? name : name.substring(idx + 1)) + ".class";
        }

        String uri = cls.getResource(classFileName).toString();
        if (uri.startsWith("file:")) {
            if (errIfFromDirectory) {
                return uri;
            } else {
                throw new IllegalStateException(
                        "This class has been loaded from a directory and not from a jar file.");
            }
        }
        if (!uri.startsWith("jar:file:")) {
            int idx = uri.indexOf(':');
            String protocol = idx == -1 ? "(unknown)" : uri.substring(0, idx);
            throw new IllegalStateException(
                    "This class has been loaded remotely via the "
                            + protocol
                            + " protocol. Only loading from a jar on the local file system is supported.");
        }

        int idx = uri.indexOf('!');
        // Sanity check
        if (idx == -1) {
            throw new IllegalStateException(
                    "You appear to have loaded this class from a local jar file, but I can't make sense of the URL!");
        }

        try {
            String fileName =
                    URLDecoder.decode(
                            uri.substring("jar:file:".length(), idx),
                            Charset.defaultCharset().name());
            return new File(fileName).getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("Default charset doesn't exist. Your VM is borked.");
        }
    }

    /**
     * Assert that all files in the list exist and if they don't, throw a RuntimeException with a
     * list of the files that do not exist.
     *
     * @param expectedFiles files that must exist
     */
    private static void assertFilesExist(final List<File> expectedFiles) {
        final List<File> missingFiles = new ArrayList<File>();
        for (final File file : expectedFiles) {
            if (file == null) {
                throw new RuntimeException("Null passed to assertFilesExist");
            }
            if (!file.exists()) {
                missingFiles.add(file);
            }
        }

        if (!missingFiles.isEmpty()) {
            List<String> missingAbsoluteFilenames = new ArrayList<String>(missingFiles.size());
            for (File missingFile : missingFiles) {
                missingAbsoluteFilenames.add(missingFile.getAbsolutePath());
            }
            throw new RuntimeException(
                    "The following files could not be located: "
                            + PluginUtil.join(", ", missingAbsoluteFilenames));
        }
    }

    private static String quote(final String str) {
        if (str.contains(" ")) {
            if (str.contains("\"")) {
                throw new RuntimeException("Don't know how to quote " + str);
            }
            return "\"" + str + "\"";
        }
        return str;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Shorthand checker names
    ///

    /**
     * All "built-in" Checker Framework checkers, except SubtypingChecker, start with this package
     * file path. Framework Checkers, except for SubtypingChecker, are excluded from processor
     * shorthand.
     */
    protected static final String CHECKER_BASE_PACKAGE = "org.checkerframework.checker";
    // Forward slash is used instead of File.separator because checker.jar uses / as the separator.
    protected static final String CHECKER_BASE_DIR_NAME = CHECKER_BASE_PACKAGE.replace(".", "/");

    protected static final String FULLY_QUALIFIED_SUBTYPING_CHECKER =
            org.checkerframework.common.subtyping.SubtypingChecker.class.getCanonicalName();

    protected static final String SUBTYPING_CHECKER_NAME =
            org.checkerframework.common.subtyping.SubtypingChecker.class.getSimpleName();

    /**
     * Returns true if processorString, once transformed into fully-qualified form, is present in
     * fullyQualifiedCheckerNames. Used by SourceChecker to determine whether a class is annotated
     * for any processor that is being run.
     *
     * @param processorString the name of a single processor, not a comma-separated list of
     *     processors
     * @param fullyQualifiedCheckerNames a list of fully-qualified checker names
     */
    public static boolean matchesCheckerOrSubcheckerFromList(
            final String processorString, List<String> fullyQualifiedCheckerNames) {
        if (processorString.contains(",")) {
            return false; // Do not process strings containing multiple processors.
        }

        return fullyQualifiedCheckerNames.contains(
                unshorthandProcessorNames(processorString, fullyQualifiedCheckerNames, true));
    }

    /**
     * For every "-processor" argument in args, replace its immediate successor argument using
     * unabbreviateProcessorNames
     */
    protected void replaceShorthandProcessor(final List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            final int nextIndex = i + 1;
            if (args.size() > nextIndex) {
                if (args.get(i).equals("-processor")) {
                    final String replacement =
                            unshorthandProcessorNames(
                                    args.get(nextIndex), getAllCheckerClassNames(), false);
                    args.remove(nextIndex);
                    args.add(nextIndex, replacement);
                }
            }
        }
    }

    /**
     * Returns the list of fully qualified names of the checkers found in checker.jar. This covers
     * only checkers with the name ending in "Checker". Checkers with a name ending in "Subchecker"
     * are not included in the returned list. Note however that it is possible for a checker with
     * the name ending in "Checker" to be used as a subchecker.
     */
    private List<String> getAllCheckerClassNames() {
        ArrayList<String> checkerClassNames = new ArrayList<String>();
        try {
            final JarInputStream checkerJarIs = new JarInputStream(new FileInputStream(checkerJar));
            ZipEntry entry;
            while ((entry = checkerJarIs.getNextEntry()) != null) {
                final String name = entry.getName();
                // Checkers ending in "Subchecker" are not included in this list used by CheckerMain.
                if (name.startsWith(CHECKER_BASE_DIR_NAME) && name.endsWith("Checker.class")) {
                    // Forward slash is used instead of File.separator because checker.jar uses / as the separator.
                    checkerClassNames.add(
                            PluginUtil.join(
                                    ".",
                                    name.substring(0, name.length() - ".class".length())
                                            .split("/")));
                }
            }
            checkerJarIs.close();
        } catch (IOException e) {
            // When using CheckerDevelMain we might not have a checker.jar file built yet.
            // Issue a warning instead of aborting execution.
            System.err.printf(
                    "Could not read %s. Shorthand processor names will not work.%n", checkerJar);
        }

        return checkerClassNames;
    }

    /**
     * Takes a string of comma-separated processor names, and expands any shorthands to
     * fully-qualified names from the fullyQualifiedCheckerNames list. For example:
     *
     * <pre>
     * NullnessChecker &rarr; org.checkerframework.checker.nullness.NullnessChecker
     * nullness &rarr; org.checkerframework.checker.nullness.NullnessChecker
     * NullnessChecker,RegexChecker &rarr; org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.regex.RegexChecker
     * </pre>
     *
     * Note, a processor entry only gets replaced if it contains NO "." (i.e., it is not qualified
     * by a package name) and can be found under the package org.checkerframework.checker in
     * checker.jar.
     *
     * @param processorsString a comma-separated string identifying processors
     * @param fullyQualifiedCheckerNames a list of fully-qualified checker names to match
     *     processorsString against
     * @param allowSubcheckers whether to match against fully qualified checker names ending with
     *     "Subchecker"
     * @return processorsString where all shorthand references to Checker Framework built-in
     *     checkers are replaced with fully-qualified references
     */
    protected static String unshorthandProcessorNames(
            final String processorsString,
            List<String> fullyQualifiedCheckerNames,
            boolean allowSubcheckers) {
        final String[] processors = processorsString.split(",");
        for (int i = 0; i < processors.length; i++) {
            if (processors[i].equals(SUBTYPING_CHECKER_NAME)) { // Allow "subtyping" as well.
                processors[i] = FULLY_QUALIFIED_SUBTYPING_CHECKER;
            } else {
                if (!processors[i].contains(".")) { // Not already fully qualified
                    processors[i] =
                            unshorthandProcessorName(
                                    processors[i], fullyQualifiedCheckerNames, allowSubcheckers);
                }
            }
        }

        return PluginUtil.join(",", processors);
    }

    /**
     * Given a processor name, tries to expand it to a checker in the fullyQualifiedCheckerNames
     * list. Returns that expansion, or the argument itself if the expansion fails.
     */
    private static String unshorthandProcessorName(
            final String processor,
            List<String> fullyQualifiedCheckerNames,
            boolean allowSubcheckers) {
        for (final String name : fullyQualifiedCheckerNames) {
            boolean tryMatch = false;
            String[] checkerPath =
                    name.substring(0, name.length() - "Checker".length()).split("\\.");
            String checkerNameShort = checkerPath[checkerPath.length - 1];
            String checkerName = checkerNameShort + "Checker";

            if (name.endsWith("Checker")) {
                checkerPath = name.substring(0, name.length() - "Checker".length()).split("\\.");
                checkerNameShort = checkerPath[checkerPath.length - 1];
                checkerName = checkerNameShort + "Checker";
                tryMatch = true;
            } else if (allowSubcheckers && name.endsWith("Subchecker")) {
                checkerPath = name.substring(0, name.length() - "Subchecker".length()).split("\\.");
                checkerNameShort = checkerPath[checkerPath.length - 1];
                checkerName = checkerNameShort + "Subchecker";
                tryMatch = true;
            }

            if (tryMatch) {
                if (processor.equalsIgnoreCase(checkerName)
                        || processor.equalsIgnoreCase(checkerNameShort)) {
                    return name;
                }
            }
        }

        return processor; // If not matched, return the input string.
    }

    /**
     * Given a shorthand processor name, returns true if it can be expanded to a checker in the
     * fullyQualifiedCheckerNames list. Does not match the subtyping checker.
     *
     * @param processor a string identifying one processor
     * @param fullyQualifiedCheckerNames a list of fully-qualified checker names to match processor
     *     against
     * @param allowSubcheckers whether to match against fully qualified checker names ending with
     *     "Subchecker"
     */
    public static boolean matchesFullyQualifiedProcessor(
            final String processor,
            List<String> fullyQualifiedCheckerNames,
            boolean allowSubcheckers) {
        return !processor.equals(
                unshorthandProcessorName(processor, fullyQualifiedCheckerNames, allowSubcheckers));
    }
}
