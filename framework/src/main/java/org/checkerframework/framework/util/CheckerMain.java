package org.checkerframework.framework.util;

import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.UserError;
import org.plumelib.util.CollectionsPlume;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * This class behaves similarly to javac. CheckerMain does the following:
 *
 * <ul>
 *   <li>add the {@code javac.jar} to the runtime classpath of the process that runs the Checker
 *       Framework.
 *   <li>parse and implement any special options used by the Checker Framework, e.g., using
 *       "shortnames" for annotation processors
 *   <li>pass all remaining command-line arguments to the real javac
 * </ul>
 *
 * To debug this class, use the {@code -AoutputArgsToFile=FILENAME} command-line argument or {@code
 * -AoutputArgsToFile=-} to output to standard out.
 *
 * <p>"To run the Checker Framework" really means to run java, where the program being run is javac
 * and javac is passed a {@code -processor} command-line argument that mentions a Checker Framework
 * checker. There are 5 relevant classpaths: The classpath and bootclasspath when running java, and
 * the classpath, bootclasspath, and processorpath used by javac. The latter three are the only
 * important ones.
 *
 * <p>Note for developers: Try to limit the work done (and options interpreted) by CheckerMain,
 * because its functionality is not available to users who choose not to use the Checker Framework
 * javac script.
 */
public class CheckerMain {

    /**
     * Any exception thrown by the Checker Framework escapes to the command line.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        final File pathToThisJar = new File(findPathTo(CheckerMain.class, false));
        ArrayList<String> alargs = new ArrayList<>(Arrays.asList(args));
        final CheckerMain program = new CheckerMain(pathToThisJar, alargs);
        final int exitStatus = program.invokeCompiler();
        System.exit(exitStatus);
    }

    /** The path to the javacJar to use. */
    protected final File javacJar;

    /** The path to the jar containing CheckerMain.class (i.e. checker.jar). */
    protected final File checkerJar;

    /** The path to checker-qual.jar. */
    protected final File checkerQualJar;

    /** The path to checker-util.jar. */
    protected final File checkerUtilJar;

    /** Compilation bootclasspath. */
    private final List<String> compilationBootclasspath;

    private final List<String> runtimeClasspath;

    private final List<String> jvmOpts;

    /**
     * Each element is either a classpath element (a directory or jar file) or is a classpath
     * (containing elements separated by File.pathSeparator). To produce the final classpath,
     * concatenate them all (separated by File.pathSeparator).
     */
    private final List<String> cpOpts;

    /** Processor path options. */
    private final List<String> ppOpts;

    /** Arguments to the Checker Framework. */
    private final List<String> toolOpts;

    /** Command-line argument files (specified with @ on the command line). */
    private final List<File> argListFiles;

    /**
     * Option name for specifying an alternative checker-qual.jar location. The accompanying value
     * MUST be the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String CHECKER_QUAL_PATH_OPT = "-checkerQualJar";

    /**
     * Option name for specifying an alternative checker-util.jar location. The accompanying value
     * MUST be the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String CHECKER_UTIL_PATH_OPT = "-checkerUtilJar";

    /**
     * Option name for specifying an alternative javac.jar location. The accompanying value MUST be
     * the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String JAVAC_PATH_OPT = "-javacJar";

    /**
     * Option name for specifying an alternative jdk.jar location. The accompanying value MUST be
     * the path to the jar file (NOT the path to its encompassing directory)
     */
    public static final String JDK_PATH_OPT = "-jdkJar";

    /**
     * Construct all the relevant file locations and Java version given the path to this jar and a
     * set of directories in which to search for jars.
     */
    public CheckerMain(final File checkerJar, final List<String> args) {

        this.checkerJar = checkerJar;
        final File searchPath = checkerJar.getParentFile();

        replaceShorthandProcessor(args);
        argListFiles = collectArgFiles(args);

        this.checkerQualJar =
                extractFileArg(
                        CHECKER_QUAL_PATH_OPT, new File(searchPath, "checker-qual.jar"), args);

        this.checkerUtilJar =
                extractFileArg(
                        CHECKER_UTIL_PATH_OPT, new File(searchPath, "checker-util.jar"), args);

        this.javacJar = extractFileArg(JAVAC_PATH_OPT, new File(searchPath, "javac.jar"), args);

        this.compilationBootclasspath = createCompilationBootclasspath(args);
        this.runtimeClasspath = createRuntimeClasspath(args);
        this.jvmOpts = extractJvmOpts(args);

        this.cpOpts = createCpOpts(args);
        this.ppOpts = createPpOpts(args);
        this.toolOpts = args;

        assertValidState();
    }

    /** Assert that required jars exist. */
    protected void assertValidState() {
        if (SystemUtil.getJreVersion() < 9) {
            assertFilesExist(Arrays.asList(javacJar, checkerJar, checkerQualJar, checkerUtilJar));
        } else {
            assertFilesExist(Arrays.asList(checkerJar, checkerQualJar, checkerUtilJar));
        }
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
        return new ArrayList<>(Arrays.asList(javacJar.getAbsolutePath()));
    }

    /**
     * Returns the compilation bootclasspath from {@code argsList}.
     *
     * @param argsList args to add
     * @return the compilation bootclasspath from {@code argsList}
     */
    protected List<String> createCompilationBootclasspath(final List<String> argsList) {
        return extractBootClassPath(argsList);
    }

    protected List<String> createCpOpts(final List<String> argsList) {
        final List<String> extractedOpts = extractCpOpts(argsList);
        extractedOpts.add(0, this.checkerQualJar.getAbsolutePath());
        extractedOpts.add(0, this.checkerUtilJar.getAbsolutePath());

        return extractedOpts;
    }

    /**
     * Returns processor path options.
     *
     * <p>This method assumes that createCpOpts has already been run.
     *
     * @param argsList arguments
     * @return processor path options
     */
    protected List<String> createPpOpts(final List<String> argsList) {
        final List<String> extractedOpts = new ArrayList<>(extractPpOpts(argsList));
        if (extractedOpts.isEmpty()) {
            // If processorpath is not provided, then javac uses the classpath.
            // CheckerMain always supplies a processorpath, so if the user
            // didn't specify a processorpath, then use the classpath.
            extractedOpts.addAll(this.cpOpts);
        }
        extractedOpts.add(0, this.checkerJar.getAbsolutePath());
        extractedOpts.add(0, this.checkerUtilJar.getAbsolutePath());

        return extractedOpts;
    }

    /**
     * Return the arguments that start with @ and therefore are files that contain javac arguments.
     *
     * @param args a list of command-line arguments; is not modified
     * @return a List of files representing all arguments that started with @
     */
    protected List<File> collectArgFiles(final List<String> args) {
        final List<File> argListFiles = new ArrayList<>();
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
            throw new BugInCF(
                    "Command line contains " + argumentName + " but no value following it");
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
        final List<String> matchedArgs = new ArrayList<>();

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
     * -Xbootclasspath/p:} command-line argument.
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

    /** Matches all {@code -J} arguments. */
    protected static final Pattern JVM_OPTS_REGEX = Pattern.compile("^(?:-J)(.*)$");

    /**
     * Remove all {@code -J} arguments from {@code args} and add them to the returned list (without
     * the {@code -J} prefix).
     *
     * @param args the arguments to extract from
     * @return all {@code -J} arguments (without the {@code -J} prefix) or an empty list if there
     *     were none
     */
    protected static List<String> extractJvmOpts(final List<String> args) {
        return extractOptWithPattern(JVM_OPTS_REGEX, false, args);
    }

    /**
     * Return the last {@code -cp} or {@code -classpath} option. If no {@code -cp} or {@code
     * -classpath} arguments were present, then return the CLASSPATH environment variable (if set)
     * followed by the current directory.
     *
     * <p>Also removes all {@code -cp} and {@code -classpath} options from args.
     *
     * @param args a list of arguments to extract from; is side-effected by this
     * @return collection of classpaths to concatenate to use when calling javac.jar
     */
    protected static List<String> extractCpOpts(final List<String> args) {
        List<String> actualArgs = new ArrayList<>();

        String lastCpArg = null;

        for (int i = 0; i < args.size(); i++) {
            if ((args.get(i).equals("-cp") || args.get(i).equals("-classpath"))
                    && (i + 1 < args.size())) {
                args.remove(i);
                // Every classpath entry overrides the one before it.
                lastCpArg = args.remove(i);
                // re-process whatever is currently at element i
                i--;
            }
        }

        // The logic below is exactly what the javac script does.  If no command-line classpath is
        // specified, use the "CLASSPATH" environment variable followed by the current directory.
        if (lastCpArg == null) {
            final String systemClassPath = System.getenv("CLASSPATH");
            if (systemClassPath != null && !systemClassPath.trim().isEmpty()) {
                actualArgs.add(systemClassPath.trim());
            }

            actualArgs.add(".");
        } else {
            actualArgs.add(lastCpArg);
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
            return Collections.singletonList(path);
        } else {
            return Collections.emptyList();
        }
    }

    protected void addMainToArgs(final List<String> args) {
        args.add("com.sun.tools.javac.Main");
    }

    /** Invoke the compiler with all relevant jars on its classpath and/or bootclasspath. */
    public List<String> getExecArguments() {
        List<String> args = new ArrayList<>(jvmOpts.size() + cpOpts.size() + toolOpts.size() + 7);

        // TODO: do we need java.exe on Windows?
        final String java = "java";
        args.add(java);

        if (SystemUtil.getJreVersion() == 8) {
            args.add("-Xbootclasspath/p:" + String.join(File.pathSeparator, runtimeClasspath));
        } else {
            args.addAll(
                    Arrays.asList(
                            "--illegal-access=warn",
                            "--add-opens",
                            "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"));
        }

        args.add("-classpath");
        args.add(String.join(File.pathSeparator, runtimeClasspath));
        args.add("-ea");
        // com.sun.tools needs to be enabled separately
        args.add("-ea:com.sun.tools...");

        args.addAll(jvmOpts);

        addMainToArgs(args);

        if (!argsListHasClassPath(argListFiles)) {
            args.add("-classpath");
            args.add(quote(concatenatePaths(cpOpts)));
        }
        if (!argsListHasProcessorPath(argListFiles)) {
            args.add("-processorpath");
            args.add(quote(concatenatePaths(ppOpts)));
        }

        if (SystemUtil.getJreVersion() == 8) {
            // No classes on the compilation bootclasspath will be loaded
            // during compilation, but the classes are read by the compiler
            // without loading them.  The compiler assumes that any class on
            // this bootclasspath will be on the bootclasspath of the JVM used
            // to later run the classfiles that Javac produces.
            args.add(
                    "-Xbootclasspath/p:"
                            + String.join(File.pathSeparator, compilationBootclasspath));

            // We currently provide a Java 8 JDK and want to be runnable
            // on a Java 8 JVM. So set source/target to 8.
            args.add("-source");
            args.add("8");
            args.add("-target");
            args.add("8");
        }

        args.addAll(toolOpts);
        return args;
    }

    /** Given a list of paths, concatenate them to form a single path. Also expand wildcards. */
    private String concatenatePaths(List<String> paths) {
        List<String> elements = new ArrayList<>();
        for (String path : paths) {
            for (String element : path.split(File.pathSeparator)) {
                elements.addAll(expandWildcards(element));
            }
        }
        return String.join(File.pathSeparator, elements);
    }

    /** The string "/*" (on Unix). */
    private static final String FILESEP_STAR = File.separator + "*";

    /**
     * Given a path element that might be a wildcard, return a list of the elements it expands to.
     * If the element isn't a wildcard, return a singleton list containing the argument.
     */
    private List<String> expandWildcards(String pathElement) {
        if (pathElement.equals("*")) {
            return jarFiles(".");
        } else if (pathElement.endsWith(FILESEP_STAR)) {
            return jarFiles(pathElement.substring(0, pathElement.length() - 1));
        } else if (pathElement.equals("")) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(pathElement);
        }
    }

    /**
     * Returns all the .jar and .JAR files in the given directory.
     *
     * @param directory a directory
     * @return all the .jar and .JAR files in the given directory
     */
    private List<String> jarFiles(String directory) {
        File dir = new File(directory);
        return Arrays.asList(dir.list((d, name) -> name.endsWith(".jar") || name.endsWith(".JAR")));
    }

    /** Invoke the compiler with all relevant jars on its classpath and/or bootclasspath. */
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
                    // The problem is that the file will sometimes disappear by the time the user
                    // can look at or run the resulting script. Maven deletes the argfile very
                    // shortly after it has been handed off to javac, for example. Ideally we would
                    // print the argfile filename as a comment but the resulting file couldn't then
                    // be run as a script on Unix or Windows.
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
        final List<String> content = new ArrayList<>();
        for (final File file : files) {
            try {
                content.addAll(Files.readAllLines(file.toPath()));
            } catch (final IOException exc) {
                throw new RuntimeException("Could not open file: " + file.getAbsolutePath(), exc);
            }
        }
        return content;
    }

    /**
     * Find the jar file or directory containing the .class file from which cls was loaded.
     *
     * @param cls the class whose .class file we wish to locate; if null, CheckerMain.class
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
        /* name is something like pakkage.name.ContainingClass$ClassName. We need to turn this into ContainingClass$ClassName.class. */
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
                            + " protocol. Only loading from a jar on the local file system is"
                            + " supported.");
        }

        int idx = uri.indexOf('!');
        // Sanity check
        if (idx == -1) {
            throw new IllegalStateException(
                    "You appear to have loaded this class from a local jar file, but URI has no"
                            + " \"!\": "
                            + uri);
        }

        try {
            String fileName =
                    URLDecoder.decode(
                            uri.substring("jar:file:".length(), idx),
                            Charset.defaultCharset().name());
            return new File(fileName).getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            throw new BugInCF("Default charset doesn't exist. Your VM is borked.");
        }
    }

    /**
     * Assert that all files in the list exist and if they don't, throw a RuntimeException with a
     * list of the files that do not exist.
     *
     * @param expectedFiles files that must exist
     */
    private static void assertFilesExist(final List<File> expectedFiles) {
        final List<File> missingFiles = new ArrayList<>();
        for (final File file : expectedFiles) {
            if (file == null) {
                throw new RuntimeException("Null passed to assertFilesExist");
            }
            if (!file.exists()) {
                missingFiles.add(file);
            }
        }

        if (!missingFiles.isEmpty()) {
            if (missingFiles.size() == 1) {
                File missingFile = missingFiles.get(0);
                if (missingFile.getName().equals("javac.jar")) {
                    throw new UserError(
                            "Could not find "
                                    + missingFile.getAbsolutePath()
                                    + ". This may be because you built the Checker Framework under"
                                    + " Java 11 but are running it under Java 8.");
                }
            }
            List<String> missingAbsoluteFilenames =
                    CollectionsPlume.mapList(File::getAbsolutePath, missingFiles);
            throw new RuntimeException(
                    "The following files could not be located: "
                            + String.join(", ", missingAbsoluteFilenames));
        }
    }

    private static String quote(final String str) {
        if (str.contains(" ")) {
            if (str.contains("\"")) {
                throw new BugInCF(
                        "Don't know how to quote a string containing a double-quote character "
                                + str);
            }
            return "\"" + str + "\"";
        }
        return str;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Shorthand checker names
    ///

    /** Processor shorthand is enabled for processors in this directory in checker.jar. */
    protected static final String CHECKER_BASE_DIR_NAME = "org/checkerframework/checker/";
    /** Processor shorthand is enabled for processors in this directory in checker.jar. */
    protected static final String COMMON_BASE_DIR_NAME = "org/checkerframework/common/";

    /**
     * Returns true if processorString, once transformed into fully-qualified form, is present in
     * fullyQualifiedCheckerNames. Used by SourceChecker to determine whether a class is annotated
     * for any processor that is being run.
     *
     * @param processorString the name of a single processor, not a comma-separated list of
     *     processors
     * @param fullyQualifiedCheckerNames a list of fully-qualified checker names
     * @return true if the fully-qualified version of {@code processorString} is in {@code
     *     fullyQualifiedCheckerNames}
     */
    public static boolean matchesCheckerOrSubcheckerFromList(
            final String processorString,
            List<@FullyQualifiedName String> fullyQualifiedCheckerNames) {
        if (processorString.contains(",")) {
            return false; // Do not process strings containing multiple processors.
        }

        return fullyQualifiedCheckerNames.contains(
                unshorthandProcessorNames(processorString, fullyQualifiedCheckerNames, true));
    }

    /**
     * For every "-processor" argument in args, replace its immediate successor argument using
     * unabbreviateProcessorNames.
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
     *
     * @return fully qualified names of the checkers found in checker.jar
     */
    private List<@FullyQualifiedName String> getAllCheckerClassNames() {
        ArrayList<@FullyQualifiedName String> checkerClassNames = new ArrayList<>();
        try {
            final JarInputStream checkerJarIs = new JarInputStream(new FileInputStream(checkerJar));
            ZipEntry entry;
            while ((entry = checkerJarIs.getNextEntry()) != null) {
                final String name = entry.getName();
                // Checkers ending in "Subchecker" are not included in this list used by
                // CheckerMain.
                if ((name.startsWith(CHECKER_BASE_DIR_NAME)
                                || name.startsWith(COMMON_BASE_DIR_NAME))
                        && name.endsWith("Checker.class")) {
                    // Forward slash is used instead of File.separator because checker.jar uses / as
                    // the separator.
                    @SuppressWarnings("signature") // string manipulation
                    @FullyQualifiedName String fqName =
                            String.join(
                                    ".",
                                    name.substring(0, name.length() - ".class".length())
                                            .split("/"));
                    checkerClassNames.add(fqName);
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
            List<@FullyQualifiedName String> fullyQualifiedCheckerNames,
            boolean allowSubcheckers) {
        final String[] processors = processorsString.split(",");
        for (int i = 0; i < processors.length; i++) {
            if (!processors[i].contains(".")) { // Not already fully qualified
                processors[i] =
                        unshorthandProcessorName(
                                processors[i], fullyQualifiedCheckerNames, allowSubcheckers);
            }
        }

        return String.join(",", processors);
    }

    /**
     * Given a processor name, tries to expand it to a checker in the fullyQualifiedCheckerNames
     * list. Returns that expansion, or the argument itself if the expansion fails.
     *
     * @param processorName a processor name, possibly in shorthand
     * @param fullyQualifiedCheckerNames all checker names
     * @param allowSubcheckers whether to match subcheckers as well as checkers
     * @return the fully-qualified version of {@code processorName} in {@code
     *     fullyQualifiedCheckerNames}, or else {@code processorName} itself
     */
    private static String unshorthandProcessorName(
            final String processorName,
            List<@FullyQualifiedName String> fullyQualifiedCheckerNames,
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
                if (processorName.equalsIgnoreCase(checkerName)
                        || processorName.equalsIgnoreCase(checkerNameShort)) {
                    return name;
                }
            }
        }

        return processorName; // If not matched, return the input string.
    }

    /**
     * Given a shorthand processor name, returns true if it can be expanded to a checker in the
     * fullyQualifiedCheckerNames list.
     *
     * @param processorName a string identifying one processor
     * @param fullyQualifiedCheckerNames a list of fully-qualified checker names to match
     *     processorName against
     * @param allowSubcheckers whether to match against fully qualified checker names ending with
     *     "Subchecker"
     * @return true if the shorthand processor name can be expanded to a checker in {@code
     *     fullyQualifiedCheckerNames}
     */
    public static boolean matchesFullyQualifiedProcessor(
            final String processorName,
            List<@FullyQualifiedName String> fullyQualifiedCheckerNames,
            boolean allowSubcheckers) {
        return !processorName.equals(
                unshorthandProcessorName(
                        processorName, fullyQualifiedCheckerNames, allowSubcheckers));
    }
}
