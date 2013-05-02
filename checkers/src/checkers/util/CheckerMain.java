package checkers.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class functions essentially the same as the JSR308 javac script EXCEPT that it adds the appropriate jdk.jar
 * to the bootclasspath and adds checkers.jar to the classpath passed to javac
 */
public class CheckerMain {

    /**
     * Most logic of the CheckerMain main method is delegated to the CheckerMain class.  This method
     * just determines the relevant parameters to CheckerMain then tells it to invoke the JSR308
     * Type Annotations Compiler
     * @param args Command line arguments, eventually passed to the jsr308 type annotations compiler
     * @throws Exception Any exception thrown by the Checker Framework escape to the command line
     */
    public static void main(String[] args)  {
        final File pathToThisJar     = new File(findPathTo(CheckerMain.class, false));
        final CheckerMain program      = new CheckerMain(pathToThisJar, args);
        final int exitStatus = program.invokeCompiler();
        System.exit(exitStatus);
    }

    /**
     * The path to the annotated jdk jar to use
     */
    private final File jdkJar;

    /**
     * The path to the jsr308 Langtools Type Annotations Compiler
     */
    private final File javacJar;

    /**
     * The paths to the jar containing CheckerMain.class (i.e. checkers.jar)
     */
    private final File checkersJar;

    /**
     * The current major version of the jre in the form 1.X where X is the major version of Java
     */
    private final double jreVersion;


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
        this.jreVersion  = getJreVersion();
        this.checkersJar   = checkersJar;

        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        argListFiles = collectArgLists(argsList);

        this.javacJar = extractFileArg(PluginUtil.JAVAC_PATH_OPT, new File(searchPath, "javac.jar"), argsList);
        this.jdkJar   = extractFileArg(PluginUtil.JDK_PATH_OPT, new File(searchPath, findJdkJarName()), argsList);

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
        for ( final String arg : args ) {
            if(arg.startsWith("@")) {
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
        for(int i = 0; i < args.size(); i++) {
            if(args.get(i).trim().equals(argumentName)) {
                if(i == args.size() - 1) {
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
        if(filePath == null) {
            return alternative;
        } else {
            return new File(filePath);
        }
    }

    /**
     * Construct a file path from files nad prepend it to previous (if previous is not null)
     * @param previous The previous file path to append to (can be null)
     * @param files    The files used to construct a path using File.pathSeparator
     * @return previous with the conjoined file path appended to it or just the conjoined file path if previous is null
     */

    protected static String prepFilePath(final String previous, File... files) {
        if(files == null || files.length == 0) {
            throw new RuntimeException("Prepending empty or null array to file path! files == " + (files == null ? " null" : " Empty"));
        } else {
            String path = files[0].getAbsolutePath();
            for( int i = 1; i < files.length; i++ ) {
                path += File.pathSeparator + files[i].getAbsolutePath();
            }

            if(previous == null || previous.isEmpty()) {
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
        while(i < args.size()) {
            final Matcher matcher = pattern.matcher(args.get(i));
            if( matcher.matches() ) {
                final String arg = matcher.group(1).trim();

                if( !arg.isEmpty() || allowEmpties ) {
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
        while(i < args.size()) {

            if( args.get(i).equals("-cp") || args.get(i).equals("-classpath")) {
                if(args.size() > i ) {
                    args.remove(i);
                    path = args.remove(i);
                } //else loop ends and we have a dangling -cp
            } else {
                i++;
            }
        }

        //The logic below is exactly what the javac script does
        //If it's empty use the current directory AND the "CLASSPATH" environment variable
        if( path == null ) {
            final String systemClassPath = System.getenv("CLASSPATH");
            if(systemClassPath != null && !systemClassPath.trim().isEmpty()) {
                actualArgs.add(System.getenv("CLASSPATH"));
            }

            actualArgs.add(".");
        } else {
            //Every classpath entry overrides the one before it and CLASSPATH
            actualArgs.add(path);
        }

        return actualArgs;
    }

    /**
     * Invoke the JSR308 Type Annotations Compiler with all relevant jars on it's classpath or boot classpath
     */
    protected int invokeCompiler() {
        List<String> args = new ArrayList<String>(jvmOpts.size() + cpOpts.size() + toolOpts.size() + 5);

        final String java = PluginUtil.getJavaCommand(System.getProperty("java.home"), System.out);
        args.add(java);

        args.add("-Xbootclasspath/p:" + PluginUtil.join(File.pathSeparator, runtimeBootClasspath));
        args.add("-ea:com.sun.tools...");

        args.addAll(jvmOpts);

        args.add("-jar");
        args.add(javacJar.getAbsolutePath());

        args.add("-Xbootclasspath/p:" + PluginUtil.join(File.pathSeparator, compilationBootclasspath));

        if( !argsListHasClassPath(argListFiles) ) {
            args.add("-classpath");
            args.add(quote(PluginUtil.join(File.pathSeparator, cpOpts)));

        }

        args.addAll(toolOpts);

        //Actually invoke the compiler
        return ExecUtil.execute(args.toArray(new String[args.size()]), System.out, System.err);
    }

    private static boolean argsListHasClassPath(final List<File> argListFiles) {
        for(final String arg : expandArgs(argListFiles)) {
            if(arg.contains("-classpath ") || arg.contains("-cp ")) {
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
        for(final File latest : args) {
            try {
                actualArgs.addAll(PluginUtil.readArgFile(latest));
            } catch(final IOException exc) {
                throw new RuntimeException("Could not open file: " + latest.getAbsolutePath(), exc);
            }
        }
        return actualArgs;
    }

    /**
     * Determine the version of the JRE that we are currently running and select a jdk<V>.jar where
     * <V> is the version of java that is being run (e.g. 6, 7, ...)
     * @return The jdk<V>.jar where <V> is the version of java that is being run (e.g. 6, 7, ...)
     */
    private String findJdkJarName() {
        final String fileName;
        if(jreVersion == 1.4 || jreVersion == 1.5 || jreVersion == 1.6) {
            fileName = "jdk6.jar";
        } else if(jreVersion == 1.7) {
            fileName = "jdk7.jar";
        } else if(jreVersion == 1.8) {
            fileName = "jdk8.jar";
        } else {
            throw new AssertionError("Unsupported JRE version: " + jreVersion);
        }

        return fileName;
    }

    /**
     * Extract the first two version numbers from java.version (e.g. 1.6 from 1.6.whatever)
     * @return The first two version numbers from java.version (e.g. 1.6 from 1.6.whatever)
     */
    private static double getJreVersion() {
        final Pattern versionPattern = Pattern.compile("^(\\d\\.\\d+)\\..*$");
        final String  jreVersionStr = System.getProperty("java.version");
        final Matcher versionMatcher = versionPattern.matcher(jreVersionStr);

        final double version;
        if(versionMatcher.matches()) {
            version = Double.parseDouble(versionMatcher.group(1));
        } else {
            throw new RuntimeException("Could not determine version from property java.version=" + jreVersionStr);
        }

        return version;
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
        /* rawName is something like package.name.ContainingClass$ClassName. We need to turn this into ContainingClass$ClassName.class. */ {
            int idx = rawName.lastIndexOf('.');
            classFileName = (idx == -1 ? rawName : rawName.substring(idx+1)) + ".class";
        }

        String uri = context.getResource(classFileName).toString();
        if (uri.startsWith("file:")) {
            if(directory) {
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
        for(final File file : expectedFiles) {
            if( file == null || !file.exists() ) {
                missingFiles.add(file);
            }
        }

        if( !missingFiles.isEmpty() ) {
            final File firstMissing = missingFiles.remove(0);
            String message = "The following files could not be located: " + firstMissing.getAbsolutePath();

            for(final File missing : missingFiles) {
                message += ", " + missing.getAbsolutePath();
            }


            throw new RuntimeException(message);
        }
    }

    private static String quote(final String str) {
        if(str.contains(" ")) {
            return "\"" + str + "\"";
        }
        return str;
    }

    private static List<String> quoteInPlace(final List<String> strings) {
        for(int i = 0; i < strings.size(); i++) {
            final String cur = strings.get(i);
            strings.add(i, quote(cur));
            strings.remove(i+1);
        }
        return strings;
    }
}
