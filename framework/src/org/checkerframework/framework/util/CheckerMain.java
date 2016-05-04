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
 * This class functions essentially the same as the jsr308-langtools javac
 * script EXCEPT that it adds the appropriate jdk.jar to the bootclasspath and
 * adds checker.jar to the classpath passed to javac.
 */
public class CheckerMain {

    /**
     * Most logic of the CheckerMain main method is delegated to the CheckerMain class.  This method
     * just determines the relevant parameters to CheckerMain then tells it to invoke the JSR 308
     * Type Annotations Compiler.
     * Any exception thrown by the Checker Framework escapes to the command line
     * @param args command-line arguments, eventually passed to the JSR 308 Type Annotations compiler
     */
    public static void main(String[] args)  {
        final File pathToThisJar    = new File(findPathTo(CheckerMain.class, false));
        final CheckerMain program   = new CheckerMain(pathToThisJar, args);
        final int exitStatus = program.invokeCompiler();
        System.exit(exitStatus);
    }

    /**
     * The path to the annotated jdk jar to use
     */
    protected final File jdkJar;

    /**
     * The path to the jsr308 Langtools Type Annotations Compiler
     */
    protected final File javacJar;

    /**
     * The paths to the jar containing CheckerMain.class (i.e. checker.jar)
     */
    protected final File checkersJar;


    private final List<String> compilationBootclasspath;

    private final List<String> runtimeBootClasspath;

    private final List<String> jvmOpts;

    private final List<String> cpOpts;

    private final List<String> toolOpts;

    private final List<File> argListFiles;

    /**
     * Construct all the relevant file locations and java version given the path to this jar and
     * a set of directories in which to search for jars
     */
    public CheckerMain(final File checkersJar, final String [] args) {

        final File searchPath = checkersJar.getParentFile();
        this.checkersJar   = checkersJar;

        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        replaceShorthandProcessor(argsList);
        argListFiles = collectArgLists(argsList);

        this.javacJar = extractFileArg(PluginUtil.JAVAC_PATH_OPT, new File(searchPath, "javac.jar"), argsList);

        final String jdkJarName = PluginUtil.getJdkJarName();
        this.jdkJar   = extractFileArg(PluginUtil.JDK_PATH_OPT, new File(searchPath, jdkJarName), argsList);

        this.compilationBootclasspath = createCompilationBootclasspath(argsList);
        this.runtimeBootClasspath     = createRuntimeBootclasspath(argsList);
        this.jvmOpts       = extractJvmOpts(argsList);

        this.cpOpts        = createCpOpts(argsList);
        this.toolOpts      = argsList;

        assertValidState();
    }

    protected void assertValidState() {
        assertFilesExist(Arrays.asList(javacJar, jdkJar, checkersJar));
    }

    /**
     * For every "-processor" argument in args, replace its immediate successor argument using
     * asCheckerFrameworkProcessors
     */
    protected void replaceShorthandProcessor(final List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            final int nextIndex = i + 1;
            if (args.size() > nextIndex) {
                if (args.get(i).equals("-processor")) {
                    final String replacement = asCheckerFrameworkProcessors(args.get(nextIndex),
                                                   getCheckerClassNames(), false);
                    args.remove(nextIndex);
                    args.add(nextIndex, replacement);
                }
            }
        }
    }

    public void addToClasspath(List<String> cpOpts) {
        this.cpOpts.addAll(cpOpts);
    }

    public void addToRuntimeBootclasspath(List<String> runtimeBootClasspathOpts) {
        this.runtimeBootClasspath.addAll(runtimeBootClasspathOpts);
    }

    protected List<String> createRuntimeBootclasspath(final List<String> argsList) {
        return new ArrayList<String>(Arrays.asList(javacJar.getAbsolutePath()));
    }

    protected List<String> createCompilationBootclasspath(final List<String> argsList) {
        final List<String> extractedBcp = extractBootClassPath(argsList);
        extractedBcp.add(0, jdkJar.getAbsolutePath());

        return extractedBcp;
    }

    protected List<String> createCpOpts(final List<String> argsList) {
        final List<String> extractedOps = extractCpOpts(argsList);
        extractedOps.add(0, this.checkersJar.getAbsolutePath());
        return extractedOps;
    }

    /**
     * Record (but don't remove) the arguments that start with @ and therefore
     * are files that contain javac arguments
     * @param args A list of command line arguments
     * @return A List of files representing all arguments that started with @
     */
    protected List<File> collectArgLists(final List<String> args) {
        final List<File> argListFiles = new ArrayList<File>();
        for (final String arg : args) {
            if (arg.startsWith("@")) {
                argListFiles.add( new File(arg.substring(1)) );
            }
        }

        return argListFiles;
    }

    /**
     * Remove the argument given by argumentName and the subsequent value from the list args if present.
     * Return the subsequent value.
     * @param argumentName Argument to extract
     * @param alternative  Value to return if argumentName is not found in args
     * @param args The current list of arguments
     * @return The string that follows argumentName if argumentName is in args or alternative if
     * argumentName is not present in args
     */
    protected static String extractArg(final String argumentName, final String alternative, final List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).trim().equals(argumentName)) {
                if (i == args.size() - 1) {
                    throw new RuntimeException("Argument " + argumentName + " specified but given no value!");
                } else {
                    args.remove(i);
                    return args.remove(i);
                }
            }
        }

        return alternative;
    }

    /**
     * Remove the argument given by argumentName and the subsequent value from the list args if present.
     * Return the subsequent value wrapped as a File.
     * @param argumentName Argument to extract
     * @param alternative  File to return if argumentName is not found in args
     * @param args The current list of arguments
     * @return The string that follows argumentName wrapped as a File if argumentName is in args or alternative if
     * argumentName is not present in args
     */
    protected static File extractFileArg(final String argumentName, final File alternative, final List<String> args) {
        final String filePath = extractArg(argumentName, null, args);
        if (filePath == null) {
            return alternative;
        } else {
            return new File(filePath);
        }
    }

    /**
     * Construct a file path from files and prepend it to previous (if previous is not null)
     * @param previous The previous file path to append to (can be null)
     * @param files    The files used to construct a path using File.pathSeparator
     * @return previous with the conjoined file path appended to it or just the conjoined file path if previous is null
     */

    protected static String prepFilePath(final String previous, File... files) {
        if (files == null || files.length == 0) {
            throw new RuntimeException("Prepending empty or null array to file path! files == " + (files == null ? " null" : " Empty"));
        } else {
            String path = files[0].getAbsolutePath();
            for (int i = 1; i < files.length; i++) {
                path += File.pathSeparator + files[i].getAbsolutePath();
            }

            if (previous == null || previous.isEmpty()) {
                return path;
            } else {
                return path + File.pathSeparator + previous;
            }
        }
    }

    /**
     * Find all args that match the given pattern and extract their index 1 group.  Add all the index 1 groups to the
     * returned list.   Remove all matching args from the input args list.
     * @param pattern      A pattern with at least one matching group
     * @param allowEmpties Whether or not to add empty group(1) matches to the returned list
     * @param args         The arguments to extract from
     * @return A list of arguments from the first group that matched the pattern for each input args or the empty list
     *         if there were none
     */
    protected static List<String> extractOptWPattern(final Pattern pattern, boolean allowEmpties, final List<String> args) {
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
     * A pattern to catch bootclasspath prepend entries, used to construct one -Xbootclasspath/p: argument
     */
    protected static final Pattern BOOT_CLASS_PATH_REGEX = Pattern.compile("^(?:-J){0,1}-Xbootclasspath/p:(.*)$");

    /**
     * Remove all -Xbootclasspath/p: or -J-Xbootclasspath/p: arguments from args and add them to the returned list
     * @param args The arguments to extract from
     * @return All non-empty arguments matching BOOT_CLASS_PATH_REGEX or an empty list if there were none
     */
    protected static List<String> extractBootClassPath(final List<String> args) {
        return extractOptWPattern(BOOT_CLASS_PATH_REGEX, false, args);
    }

    /**
     * Matches all -J arguments
     */
    protected static final Pattern JVM_OPTS_REGEX = Pattern.compile("^(?:-J)(.*)$");

    /**
     * Remove all -J arguments from args and add them to the returned list
     * @param args The arguments to extract from
     * @return All -j arguments (without the -J prefix) or an empty list if there were none
     */
    protected static List<String> extractJvmOpts(final List<String> args) {
        return extractOptWPattern(JVM_OPTS_REGEX, false, args);
    }

    /**
     * Extract the -cp and -classpath arguments and there immediate predecessors in args.  Return a list of the
     * predecessors.  If NO -cp or -classpath arguments were present then use the current directory and the
     * CLASSPATH environment variable
     * @param args A list of arguments to extract from
     * @return The arguments that should be put on the classpath when calling javac.jar
     */
    protected static List<String> extractCpOpts(final List<String> args) {
        List<String> actualArgs = new ArrayList<String>();

        String path = null;

        int i = 0;
        while (i < args.size()) {

            if (args.get(i).equals("-cp") || args.get(i).equals("-classpath")) {
                if (args.size() > i) {
                    args.remove(i);
                    path = args.remove(i);
                } // else loop ends and we have a dangling -cp
            } else {
                i++;
            }
        }

        //The logic below is exactly what the javac script does
        //If it's empty use the current directory AND the "CLASSPATH" environment variable
        if (path == null) {
            final String systemClassPath = System.getenv("CLASSPATH");
            if (systemClassPath != null && !systemClassPath.trim().isEmpty()) {
                actualArgs.add(System.getenv("CLASSPATH"));
            }

            actualArgs.add(".");
        } else {
            //Every classpath entry overrides the one before it and CLASSPATH
            actualArgs.add(path);
        }

        return actualArgs;
    }

    protected void addMainArgs(final List<String> args) {
        args.add("com.sun.tools.javac.Main");
    }

    /**
     * Invoke the JSR308 Type Annotations Compiler with all relevant jars on it's classpath or boot classpath
     */
    public List<String> getExecArguments() {
        List<String> args = new ArrayList<String>(jvmOpts.size() + cpOpts.size() + toolOpts.size() + 5);

        final String java = PluginUtil.getJavaCommand(System.getProperty("java.home"), System.out);
        args.add(java);

        args.add("-Xbootclasspath/p:" + PluginUtil.join(File.pathSeparator, runtimeBootClasspath));
        args.add("-ea");
        // com.sun.tools needs to be enabled separately
        args.add("-ea:com.sun.tools...");

        args.addAll(jvmOpts);

        addMainArgs(args);

        args.add("-Xbootclasspath/p:" + PluginUtil.join(File.pathSeparator, compilationBootclasspath));

        if (!argsListHasClassPath(argListFiles)) {
            args.add("-classpath");
            args.add(quote(PluginUtil.join(File.pathSeparator, cpOpts)));
        }

        args.addAll(toolOpts);
        return args;
    }

    /**
     * Invoke the JSR308 Type Annotations Compiler with all relevant jars on it's classpath or boot classpath
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

        //Actually invoke the compiler
        return ExecUtil.execute(args.toArray(new String[args.size()]), System.out, System.err);
    }

    private static void outputArgumentsToFile(String outputFilename, List<String> args) {
        if (outputFilename != null) {
            String errorMessage = null;

            try {
                PrintWriter writer = new PrintWriter(outputFilename, "UTF-8");
                for (int i = 0; i < args.size(); i++) {
                    String arg = args.get(i);

                    // We would like to include the filename of the argfile instead of its contents.
                    // The problem is that the file will sometimes disappear by the time the user can
                    // look at or run the resulting script. Maven deletes the argfile very shortly
                    // after it has been handed off to javac, for example. Ideally we would print
                    // the argfile filename as a comment but the resulting file couldn't then be run as
                    // a script on Unix or Windows.
                    if (arg.startsWith("@")) { // Read argfile and include its parameters in the output file.
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
            }
            catch (IOException e) {
                errorMessage = e.toString();
            }

            if (errorMessage != null) {
                System.err.println("Failed to output command-line arguments to file " + outputFilename + " due to exception: " + errorMessage);
            }
        }
    }

    private static boolean argsListHasClassPath(final List<File> argListFiles) {
        for (final String arg : expandArgs(argListFiles)) {
            if (arg.contains("-classpath ") || arg.contains("-cp ")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Iterate through the arguments and, for every argument that starts with an @, replace it
     * with the lines contained by that file.
     * @param args A list of arguments, which may or may not be prefixed with an @
     * @return A list of args with no @ symbols, where every argument that started with an @
     * symbol has been replaced with the list of lines of the files content
     */
    protected static List<String> expandArgs(final List<File> args)  {
        final List<String> actualArgs = new ArrayList<String>();
        for (final File latest : args) {
            try {
                actualArgs.addAll(PluginUtil.readArgFile(latest));
            } catch (final IOException exc) {
                throw new RuntimeException("Could not open file: " + latest.getAbsolutePath(), exc);
            }
        }
        return actualArgs;
    }

    /**
     * Find the jar file or directory containing the .class file from which context was loaded
     * @param context The class whose .class file we wish to locate
     * @param directory Whether to throw an exception if the file was loaded from a directory
     */
    public static String findPathTo(Class<?> context, boolean directory) throws IllegalStateException {
        if (context == null) context = CheckerMain.class;
        String rawName = context.getName();
        String classFileName;
        /* rawName is something like package.name.ContainingClass$ClassName. We need to turn this into ContainingClass$ClassName.class. */
        {
            int idx = rawName.lastIndexOf('.');
            classFileName = (idx == -1 ? rawName : rawName.substring(idx+1)) + ".class";
        }

        String uri = context.getResource(classFileName).toString();
        if (uri.startsWith("file:")) {
            if (directory) {
                return uri;
            } else {
                throw new IllegalStateException("This class has been loaded from a directory and not from a jar file.");
            }
        }
        if (!uri.startsWith("jar:file:")) {
            int idx = uri.indexOf(':');
            String protocol = idx == -1 ? "(unknown)" : uri.substring(0, idx);
            throw new IllegalStateException("This class has been loaded remotely via the " + protocol +
                    " protocol. Only loading from a jar on the local file system is supported.");
        }

        int idx = uri.indexOf('!');
        //As far as I know, the if statement below can't ever trigger, so it's more of a sanity check thing.
        if (idx == -1) throw new IllegalStateException("You appear to have loaded this class from a local jar file, but I can't make sense of the URL!");

        try {
            String fileName = URLDecoder.decode(uri.substring("jar:file:".length(), idx), Charset.defaultCharset().name());
            return new File(fileName).getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("default charset doesn't exist. Your VM is borked.");
        }
    }

    /**
     * Assert that all files in the list exist and if they don't, throw a RuntimeException with a list of the files
     * that do not exist
     *
     * @param expectedFiles Files that must exist
     */
    private static void assertFilesExist(final List<File> expectedFiles) {
        final List<File> missingFiles = new ArrayList<File>();
        for (final File file : expectedFiles) {
            if (file == null || !file.exists()) {
                missingFiles.add(file);
            }
        }

        if (!missingFiles.isEmpty()) {
            final File firstMissing = missingFiles.remove(0);
            String message = "The following files could not be located: " + firstMissing.getAbsolutePath();

            for (final File missing : missingFiles) {
                message += ", " + missing.getAbsolutePath();
            }

            throw new RuntimeException(message);
        }
    }

    private static String quote(final String str) {
        if (str.contains(" ")) {
            return "\"" + str + "\"";
        }
        return str;
    }

    /**
     * All "built-in" Checker Framework checkers, except SubtypingChecker, start with this package file path
     * Framework Checkers, except for SubtypingChecker are excluded from processor shorthand
     */
    protected static final String CHECKER_BASE_PACKAGE = "org.checkerframework.checker";
    // Forward slash is used instead of File.separator because checker.jar uses / as the separator.
    protected static final String CHECKER_BASE_DIR_NAME = CHECKER_BASE_PACKAGE.replace(".", "/");

    protected static final String FULLY_QUALIFIED_SUBTYPING_CHECKER =
            org.checkerframework.common.subtyping.SubtypingChecker.class.getCanonicalName();

    protected static final String SUBTYPING_CHECKER_NAME =
            org.checkerframework.common.subtyping.SubtypingChecker.class.getSimpleName();

    // Returns true if processorString, once transformed into fully qualified form, is present
    // in fullyQualifiedCheckerNames.
    // processorString must be the name of a single processor, not a comma-separated list of processors.
    public static boolean matchesCheckerOrSubcheckerFromList(final String processorString, List<String> fullyQualifiedCheckerNames) {
        if (processorString.contains(",")) {
            return false; // Do not process strings containing multiple processors.
        }

        return fullyQualifiedCheckerNames.contains(asCheckerFrameworkProcessors(processorString, fullyQualifiedCheckerNames, true));
    }

    // Returns the list of fully qualified names of the checkers found in checker.jar
    // This covers only checkers with the name ending in "Checker"
    // Checkers with a name ending in "Subchecker" are not included in the returned list,
    // Note however that it is possible for a checker with the name ending in "Checker" to be used as a subchecker.
    private List<String> getCheckerClassNames() {
        ArrayList<String> checkerClassNames = new ArrayList<String>();
        try {
            final JarInputStream checkerJarIs = new JarInputStream(new FileInputStream(checkersJar));
            ZipEntry entry;
            while ((entry = checkerJarIs.getNextEntry()) != null) {
                final String name = entry.getName();
                if (name.startsWith(CHECKER_BASE_DIR_NAME) && name.endsWith("Checker.class")) { // Checkers ending in "Subchecker" are not included in this list used by CheckerMain.
                    // Forward slash is used instead of File.separator because checker.jar uses / as the separator.
                    checkerClassNames.add(PluginUtil.join(".", name.substring(0, name.length() - ".class".length()).split("/")));
                }
            }
            checkerJarIs.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not read checker.jar", e);
        }

        return checkerClassNames;
    }

    /**
     * Takes a processor string of the form
     * NullnessChecker
     * or
     * nullness
     * and returns:
     * org.checkerframework.checker.nullness.NullnessChecker
     * if the latter is found in the fullyQualifiedCheckerNames list. Also fully qualifies the subtyping checker.
     *
     * asCheckerFrameworkProcessors will handle processor strings with multiple processors, e.g:
     * NullnessChecker,RegexChecker
     * becomes:
     * org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.regex.RegexChecker
     *
     * Note, a processor entry only gets replaced if it contains NO "." (i.e. is not qualified by the
     * package name) and can be found under the package org.checkerframework.checker in the checker.jar.
     * @param processorsString A string identifying processors
     * @param fullyQualifiedCheckerNames A list of fully-qualified checker names to match processorsString against.
     * @param allowSubcheckers Whether to match against fully qualified checker names ending with "Subchecker".
     * @return processorsString where all unqualified references to Checker Framework built-in checkers
     * are replaced with fully-qualified references
     */
    protected static String asCheckerFrameworkProcessors(final String processorsString, List<String> fullyQualifiedCheckerNames, boolean allowSubcheckers) {
        final String[] processors = processorsString.split(",");
        for (int i = 0; i < processors.length; i++) {
            if (processors[i].equals(SUBTYPING_CHECKER_NAME)) { // Allow "subtyping" as well.
                processors[i] = FULLY_QUALIFIED_SUBTYPING_CHECKER;
            } else {
                if (!processors[i].contains(".")) { // Not already fully qualified
                    processors[i] = fullyQualifyProcessor(processors[i], fullyQualifiedCheckerNames, allowSubcheckers);
                }
            }
        }


        return PluginUtil.join(",", processors);
    }

    private static String fullyQualifyProcessor(final String processor, List<String> fullyQualifiedCheckerNames, boolean allowSubcheckers) {
        for (final String name : fullyQualifiedCheckerNames) {
            boolean tryMatch = false;
            String [] checkerPath = name.substring(0, name.length() - "Checker".length()).split("\\.");
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
                if (processor.equalsIgnoreCase(checkerName) ||
                    processor.equalsIgnoreCase(checkerNameShort)) {
                    return name;
                }
            }
        }

        return processor; // If not matched, return the input string.
    }

    /**
     * Takes a processor string of the form
     * "NullnessChecker"
     * or
     * "nullness"
     * and returns true
     * if it is found in the fullyQualifiedCheckerNames list.
     * Does not match the subtyping checker.
     *
     * Does not match multiple processors - a single processor name must be given.
     *
     * @param processor A string identifying one processor.
     * @param fullyQualifiedCheckerNames A list of fully-qualified checker names to match processor against.
     * @param allowSubcheckers Whether to match against fully qualified checker names ending with "Subchecker".
     */
    public static boolean matchesFullyQualifiedProcessor(final String processor, List<String> fullyQualifiedCheckerNames, boolean allowSubcheckers) {
        for (final String name : fullyQualifiedCheckerNames) {
            boolean tryMatch = false;
            String [] checkerPath = name.substring(0, name.length() - "Checker".length()).split("\\.");
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
                if (processor.equalsIgnoreCase(checkerName) ||
                    processor.equalsIgnoreCase(checkerNameShort)) {
                    return true;
                }
            }
        }

        return false;
    }
}
