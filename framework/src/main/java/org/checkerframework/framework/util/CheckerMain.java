package org.checkerframework.framework.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.SystemUtil;
import org.plumelib.util.CollectionsPlume;

/**
 * This class behaves similarly to javac. CheckerMain does the following:
 *
 * <ul>
 *   <li>parse and implement any special options used by the Checker Framework, e.g., using
 *       "shortnames" for annotation processors
 *   <li>pass all remaining command-line arguments to the real javac
 * </ul>
 *
 * To debug this class, use the {@code -AoutputArgsToFile=FILENAME} command-line argument or {@code
 * -AoutputArgsToFile=-} to output to standard out.
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
    File pathToThisJar = new File(findPathTo(CheckerMain.class, false));
    ArrayList<String> alargs = new ArrayList<>(Arrays.asList(args));
    CheckerMain program = new CheckerMain(pathToThisJar, alargs);
    int exitStatus = program.invokeCompiler();
    System.exit(exitStatus);
  }

  /** The path to the jar containing CheckerMain.class (i.e. checker.jar). */
  protected final File checkerJar;

  /** The path to checker-qual.jar. */
  protected final File checkerQualJar;

  /** The path to checker-util.jar. */
  protected final File checkerUtilJar;

  /** A list of JVM options. */
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
   * Construct all the relevant file locations and Java version given the path to this jar and a set
   * of directories in which to search for jars.
   */
  @SuppressWarnings("this-escape")
  public CheckerMain(File checkerJar, List<String> args) {

    this.checkerJar = checkerJar;
    File searchPath = checkerJar.getParentFile();

    replaceShorthandProcessor(args);
    argListFiles = collectArgFiles(args);

    this.checkerQualJar =
        extractFileArg(CHECKER_QUAL_PATH_OPT, new File(searchPath, "checker-qual.jar"), args);

    this.checkerUtilJar =
        extractFileArg(CHECKER_UTIL_PATH_OPT, new File(searchPath, "checker-util.jar"), args);

    this.jvmOpts = extractJvmOpts(args);

    this.cpOpts = extractCpOpts(args);
    cpOpts.add(0, this.checkerQualJar.getAbsolutePath());
    cpOpts.add(0, this.checkerUtilJar.getAbsolutePath());

    String passedProcessorPath = extractPpOpts(args);
    if (passedProcessorPath == null) {
      // If processorpath is not provided, then javac uses the classpath.
      // CheckerMain always supplies a processorpath, so if the user
      // didn't specify a processorpath, then use the classpath.
      this.ppOpts = new ArrayList<>(this.cpOpts.size() + 2);
      ppOpts.addAll(this.cpOpts);
    } else {
      this.ppOpts = new ArrayList<>(3);
      ppOpts.add(passedProcessorPath);
    }
    ppOpts.add(0, this.checkerJar.getAbsolutePath());
    ppOpts.add(0, checkerUtilJar.getAbsolutePath());

    this.toolOpts = args;

    assertValidState();
  }

  /** Assert that required jars exist. */
  protected void assertValidState() {
    assertFilesExist(checkerJar, checkerQualJar, checkerUtilJar);
  }

  /**
   * Returns the arguments that start with @ and therefore are files that contain javac arguments.
   *
   * @param args a list of command-line arguments; is not modified
   * @return a List of files representing all arguments that started with @
   */
  protected List<File> collectArgFiles(List<String> args) {
    List<File> argListFiles = new ArrayList<>();
    for (String arg : args) {
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
   * @param args the current list of arguments
   * @return the string that follows argumentName if argumentName is in args, or alternative if
   *     argumentName is not present in args
   */
  protected static @PolyNull String extractArg(String argumentName, List<String> args) {
    int i = args.indexOf(argumentName);
    if (i == -1) {
      return null;
    } else if (i == args.size() - 1) {
      throw new BugInCF("Command line contains " + argumentName + " but no value following it");
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
  protected static File extractFileArg(String argumentName, File alternative, List<String> args) {
    String filePath = extractArg(argumentName, args);
    if (filePath == null) {
      return alternative;
    } else {
      return new File(filePath);
    }
  }

  /** Matches all {@code -J} arguments. */
  protected static final Pattern JVM_OPTS_REGEX = Pattern.compile("^-J(.*)$");

  /**
   * Remove all {@code -J} arguments from {@code args} and add them to the returned list (without
   * the {@code -J} prefix).
   *
   * @param args the arguments to extract from
   * @return all {@code -J} arguments (without the {@code -J} prefix) or an empty list if there were
   *     none
   */
  protected static List<String> extractJvmOpts(List<String> args) {
    List<String> jvmArgs = new ArrayList<>();

    Iterator<String> argsIterator = args.iterator();
    while (argsIterator.hasNext()) {
      String arg = argsIterator.next();
      Matcher matcher = CheckerMain.JVM_OPTS_REGEX.matcher(arg);
      if (matcher.matches()) {
        jvmArgs.add(arg);
        argsIterator.remove();
      }
    }

    return jvmArgs;
  }

  /**
   * Returns the last {@code -cp} or {@code -classpath} option. If no {@code -cp} or {@code
   * -classpath} arguments were present, then return the CLASSPATH environment variable (if set)
   * followed by the current directory.
   *
   * <p>Also removes all {@code -cp} and {@code -classpath} options from args.
   *
   * @param args a list of arguments to extract from; is side-effected by this
   * @return collection of classpaths to concatenate to use when calling javac.jar
   */
  protected static List<String> extractCpOpts(List<String> args) {
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
      String systemClassPath = System.getenv("CLASSPATH");
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
   * Remove all {@code -processorpath} options and their arguments from args. Return the last
   * argument.
   *
   * @param args a list of arguments to extract from
   * @return the arguments that should be put on the processorpath when calling javac.jar
   */
  protected static @Nullable String extractPpOpts(List<String> args) {
    String path = null;

    for (int i = 0; i < args.size(); i++) {
      if (args.get(i).equals("-processorpath") && (i + 1 < args.size())) {
        args.remove(i);
        path = args.remove(i);
        // re-process whatever is currently at element i
        i--;
      }
    }
    return path;
  }

  /**
   * Returns a list of arguments to pass to javac.
   *
   * @return a list of arguments to pass to javac
   */
  public List<String> getExecArguments() {
    List<String> args = new ArrayList<>(jvmOpts.size() + cpOpts.size() + toolOpts.size() + 7);

    // TODO: do we need java.exe on Windows?
    String java = "javac";
    args.add(java);

    args.addAll(
        // Keep this list in sync with the lists in checker-framework/build.gradle in
        // compilerArgsForRunningCFs, the sections with labels
        // "javac-jdk11-non-modularized", "maven", and "sbt" in the manual, and in the
        // checker-framework-gradle-plugin, CheckerFrameworkPlugin#applyToProject
        Arrays.asList(
            // These are required in Java 17+ because the --illegal-access option is
            // set to deny by default.  None of these packages are accessed via
            // reflection, so the module only needs to be exported, but not opened.
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            // Required because the Checker Framework reflectively accesses private
            // members in com.sun.tools.javac.comp.
            "-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"));

    args.addAll(jvmOpts);

    if (!argsListHasClassPath(argListFiles)) {
      args.add("-classpath");
      args.add(quote(concatenatePaths(cpOpts)));
    }
    if (!argsListHasProcessorPath(argListFiles)) {
      args.add("-processorpath");
      args.add(quote(concatenatePaths(ppOpts)));
    }

    args.addAll(toolOpts);
    return args;
  }

  /** Given a list of paths, concatenate them to form a single path. Also expand wildcards. */
  private String concatenatePaths(List<String> paths) {
    List<String> elements = new ArrayList<>();
    for (String path : paths) {
      for (String element : SystemUtil.pathSeparatorSplitter.split(path)) {
        elements.add(element);
      }
    }
    return String.join(File.pathSeparator, elements);
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
    return ExecUtil.execute(args.toArray(new String[0]), System.out, System.err);
  }

  private static void outputArgumentsToFile(String outputFilename, List<String> args) {
    if (outputFilename != null) {
      String errorMessage = null;

      try {
        @SuppressWarnings("builder:required.method.not.called") // don't want to close System.out
        PrintWriter writer =
            (outputFilename.equals("-")
                ? new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)))
                : new PrintWriter(outputFilename, StandardCharsets.UTF_8));
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

            try (BufferedReader br =
                Files.newBufferedReader(Paths.get(inputFilename), StandardCharsets.UTF_8)) {
              String line;
              while ((line = br.readLine()) != null) {
                writer.print(line);
                writer.print(" ");
              }
            }
          } else {
            writer.print(arg);
            writer.print(" ");
          }
          writer.flush();
        }
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
  private static boolean argsListHasClassPath(List<File> argListFiles) {
    for (String arg : expandArgFiles(argListFiles)) {
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
  private static boolean argsListHasProcessorPath(List<File> argListFiles) {
    for (String arg : expandArgFiles(argListFiles)) {
      if (arg.contains("-processorpath")) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns all the lines in all the files.
   *
   * @param files a list of files
   * @return a list of all the lines in all the files
   */
  protected static List<String> expandArgFiles(List<File> files) {
    List<String> content = new ArrayList<>();
    for (File file : files) {
      try {
        content.addAll(Files.readAllLines(file.toPath()));
      } catch (IOException exc) {
        throw new RuntimeException("Could not open file: " + file.getAbsolutePath(), exc);
      }
    }
    return content;
  }

  /**
   * Find the jar file or directory containing the .class file from which cls was loaded.
   *
   * @param cls the class whose .class file we wish to locate; if null, CheckerMain.class
   * @param errIfFromDirectory if false, throw an exception if the file was loaded from a directory
   */
  public static String findPathTo(@Nullable Class<?> cls, boolean errIfFromDirectory)
      throws IllegalStateException {
    if (cls == null) {
      cls = CheckerMain.class;
    }
    String name = cls.getName();
    String classFileName;
    /* name is something like pakkage.name.EnclosingClass$ClassName. We need to turn this into EnclosingClass$ClassName.class. */
    {
      int idx = name.lastIndexOf('.');
      classFileName = (idx == -1 ? name : name.substring(idx + 1)) + ".class";
    }

    URL classFileUrl = cls.getResource(classFileName);
    if (classFileUrl == null) {
      throw new BugInCF("Cannot find resource " + classFileName);
    }
    if (classFileUrl.getProtocol().equals("file")) {
      if (errIfFromDirectory) {
        return classFileUrl.toString();
      } else {
        throw new IllegalStateException(
            "This class has been loaded from a directory and not from a jar file.");
      }
    }
    String uri = classFileUrl.toString();
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
          "You appear to have loaded this class from a local jar file, but URI has no \"!\": "
              + uri);
    }

    String fileName =
        URLDecoder.decode(uri.substring("jar:file:".length(), idx), Charset.defaultCharset());
    return new File(fileName).getAbsolutePath();
  }

  /**
   * Assert that all files in the list exist and if they don't, throw a RuntimeException with a list
   * of the files that do not exist.
   *
   * @param expectedFiles files that must exist
   */
  private static void assertFilesExist(File... expectedFiles) {
    List<File> missingFiles = new ArrayList<>();
    for (File file : expectedFiles) {
      if (file == null) {
        throw new RuntimeException("Null passed to assertFilesExist");
      }
      if (!file.exists()) {
        missingFiles.add(file);
      }
    }

    if (!missingFiles.isEmpty()) {
      List<String> missingAbsoluteFilenames =
          CollectionsPlume.mapList(File::getAbsolutePath, missingFiles);
      throw new RuntimeException(
          "The following files could not be located: "
              + String.join(", ", missingAbsoluteFilenames));
    }
  }

  private static String quote(String str) {
    if (str.contains(" ")) {
      if (str.contains("\"")) {
        throw new BugInCF(
            "Don't know how to quote a string containing a double-quote character " + str);
      }
      return "\"" + str + "\"";
    }
    return str;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Shorthand checker names
  //

  /** Processor shorthand is enabled for processors in this directory in checker.jar. */
  protected static final String CHECKER_BASE_DIR_NAME = "org/checkerframework/checker/";

  /** Processor shorthand is enabled for processors in this directory in checker.jar. */
  protected static final String COMMON_BASE_DIR_NAME = "org/checkerframework/common/";

  /**
   * Returns true if processorString, once transformed into fully-qualified form, is present in
   * fullyQualifiedCheckerNames. Used by SourceChecker to determine whether a class is annotated for
   * any processor that is being run.
   *
   * @param processorString the name of a single processor, not a comma-separated list of processors
   * @param fullyQualifiedCheckerNames a list of fully-qualified checker names
   * @return true if the fully-qualified version of {@code processorString} is in {@code
   *     fullyQualifiedCheckerNames}
   */
  public static boolean matchesCheckerOrSubcheckerFromList(
      String processorString, List<@FullyQualifiedName String> fullyQualifiedCheckerNames) {
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
  protected void replaceShorthandProcessor(List<String> args) {
    for (int i = 0; i < args.size(); i++) {
      int nextIndex = i + 1;
      if (args.size() > nextIndex) {
        if (args.get(i).equals("-processor")) {
          String replacement =
              unshorthandProcessorNames(args.get(nextIndex), getAllCheckerClassNames(), false);
          args.remove(nextIndex);
          args.add(nextIndex, replacement);
        }
      }
    }
  }

  /**
   * Returns the list of fully qualified names of the checkers found in checker.jar. This covers
   * only checkers with the name ending in "Checker". Checkers with a name ending in "Subchecker"
   * are not included in the returned list. Note however that it is possible for a checker with the
   * name ending in "Checker" to be used as a subchecker.
   *
   * @return fully qualified names of the checkers found in checker.jar
   */
  private List<@FullyQualifiedName String> getAllCheckerClassNames() {
    ArrayList<@FullyQualifiedName String> checkerClassNames = new ArrayList<>();
    try (FileInputStream fis = new FileInputStream(checkerJar);
        JarInputStream checkerJarIs = new JarInputStream(fis)) {
      ZipEntry entry;
      while ((entry = checkerJarIs.getNextEntry()) != null) {
        String name = entry.getName();
        // Checkers ending in "Subchecker" are not included in this list used by
        // CheckerMain.
        if ((name.startsWith(CHECKER_BASE_DIR_NAME) || name.startsWith(COMMON_BASE_DIR_NAME))
            && name.endsWith("Checker.class")) {
          // Forward slash is used instead of File.separator because checker.jar uses / as
          // the separator.
          @SuppressWarnings("signature") // string manipulation
          @FullyQualifiedName String fqName =
              String.join(".", name.substring(0, name.length() - ".class".length()).split("/"));
          checkerClassNames.add(fqName);
        }
      }
    } catch (IOException e) {
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
   * Note, a processor entry only gets replaced if it contains NO "." (i.e., it is not qualified by
   * a package name) and can be found under the package org.checkerframework.checker in checker.jar.
   *
   * @param processorsString a comma-separated string identifying processors; often just one
   *     processor
   * @param fullyQualifiedCheckerNames a list of fully-qualified checker names to match
   *     processorsString against
   * @param allowSubcheckers if true, match against fully qualified checker names ending with
   *     "Subchecker"
   * @return processorsString where all shorthand references to Checker Framework built-in checkers
   *     are replaced with fully-qualified references
   */
  protected static String unshorthandProcessorNames(
      String processorsString,
      List<@FullyQualifiedName String> fullyQualifiedCheckerNames,
      boolean allowSubcheckers) {
    StringJoiner result = new StringJoiner(",");
    for (String processor : SystemUtil.commaSplitter.split(processorsString)) {
      if (!processor.contains(".")) { // Not already fully qualified
        processor =
            unshorthandProcessorName(processor, fullyQualifiedCheckerNames, allowSubcheckers);
      }
      result.add(processor);
    }
    return result.toString();
  }

  /**
   * Given a processor name, tries to expand it to a checker in the fullyQualifiedCheckerNames list.
   * Returns that expansion, or the argument itself if the expansion fails.
   *
   * @param processorName a processor name, possibly in shorthand
   * @param fullyQualifiedCheckerNames all checker names
   * @param allowSubcheckers if true, match subcheckers as well as checkers
   * @return the fully-qualified version of {@code processorName} in {@code
   *     fullyQualifiedCheckerNames}, or else {@code processorName} itself
   */
  private static String unshorthandProcessorName(
      String processorName,
      List<@FullyQualifiedName String> fullyQualifiedCheckerNames,
      boolean allowSubcheckers) {
    for (String name : fullyQualifiedCheckerNames) {
      boolean tryMatch = false;
      String[] checkerPath = name.substring(0, name.length() - "Checker".length()).split("\\.");
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
   * @param allowSubcheckers if true, match against fully qualified checker names ending with
   *     "Subchecker"
   * @return true if the shorthand processor name can be expanded to a checker in {@code
   *     fullyQualifiedCheckerNames}
   */
  public static boolean matchesFullyQualifiedProcessor(
      String processorName,
      List<@FullyQualifiedName String> fullyQualifiedCheckerNames,
      boolean allowSubcheckers) {
    return !processorName.equals(
        unshorthandProcessorName(processorName, fullyQualifiedCheckerNames, allowSubcheckers));
  }
}
