package org.checkerframework.javacutil;

import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;

/** This file contains basic utility functions. */
public class SystemUtil {

  /** Do not instantiate. */
  private SystemUtil() {
    throw new Error("Do not instantiate.");
  }

  /** The major version number of the Java runtime (JRE), such as 8, 11, or 17. */
  @SuppressWarnings("deprecation") // remove @SuppressWarnings when getJreVersion() isn't deprecated
  public static final int jreVersion = getJreVersion();

  // Keep in sync with BCELUtil.java (in the bcel-util project).
  /**
   * Returns the major version number from the "java.version" system property, such as 8, 11, or 17.
   *
   * <p>This is different from the version passed to the compiler via --release; use {@link
   * #getReleaseValue(ProcessingEnvironment)} to get that version.
   *
   * <p>Extract the major version number from the "java.version" system property. Two possible
   * formats are considered. Up to Java 8, from a version string like `1.8.whatever`, this method
   * extracts 8. Since Java 9, from a version string like `11.0.1`, this method extracts 11.
   *
   * <p>Starting in Java 9, there is the int {@code Runtime.version().feature()}, but that does not
   * exist on JDK 8.
   *
   * @return the major version of the Java runtime
   * @deprecated use field {@link #jreVersion} instead
   */
  @Deprecated // 2022-07-14 not for removal, just to make private (and then it won't be
  // deprecated)
  public static int getJreVersion() {
    String version = System.getProperty("java.version");

    // Up to Java 8, from a version string like "1.8.whatever", extract "8".
    if (version.startsWith("1.")) {
      return Integer.parseInt(version.substring(2, 3));
    }

    // Since Java 9, from a version string like "11.0.1" or "11-ea" or "11u25", extract "11".
    // The format is described at http://openjdk.org/jeps/223 .
    Pattern newVersionPattern = Pattern.compile("^(\\d+).*$");
    Matcher newVersionMatcher = newVersionPattern.matcher(version);
    if (newVersionMatcher.matches()) {
      String v = newVersionMatcher.group(1);
      assert v != null : "@AssumeAssertion(nullness): inspection";
      return Integer.parseInt(v);
    }

    throw new RuntimeException("Could not determine version from property java.version=" + version);
  }

  /**
   * Returns the release value passed to the compiler or null if release was not passed.
   *
   * @param env the ProcessingEnvironment
   * @return the release value or null if none was passed
   */
  public static @Nullable String getReleaseValue(ProcessingEnvironment env) {
    Context ctx = ((JavacProcessingEnvironment) env).getContext();
    Options options = Options.instance(ctx);
    return options.get(Option.RELEASE);
  }

  /**
   * Returns the pathname to the tools.jar file, or null if it does not exist. Returns null on Java
   * 9 and later.
   *
   * @return the pathname to the tools.jar file, or null
   */
  public static @Nullable String getToolsJar() {

    if (jreVersion > 8) {
      return null;
    }

    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      String javaHomeProperty = System.getProperty("java.home");
      if (javaHomeProperty.endsWith(File.separator + "jre")) {
        javaHome = javaHomeProperty.substring(javaHomeProperty.length() - 4);
      } else {
        // Could also determine the location of javac on the path...
        throw new Error("Can't infer Java home; java.home=" + javaHomeProperty);
      }
    }
    File toolsJarFile = new File(new File(javaHome, "lib"), "tools.jar");
    if (!toolsJarFile.exists()) {
      throw new Error(
          String.format(
              "File does not exist: %s ; JAVA_HOME=%s ; java.home=%s",
              toolsJarFile, javaHome, System.getProperty("java.home")));
    }
    return toolsJarFile.toString();
  }

  ///
  /// Deprecated methods
  ///

  /**
   * Return a list of Strings, one per line of the file.
   *
   * @param argFile argument file
   * @return a list of Strings, one per line of the file
   * @throws IOException when reading the argFile
   * @deprecated use Files.readAllLines
   */
  @Deprecated // 2021-03-10
  public static List<String> readFile(File argFile) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(argFile))) {
      String line;
      List<String> lines = new ArrayList<>();
      while ((line = br.readLine()) != null) {
        lines.add(line);
      }
      return lines;
    }
  }

  /**
   * Like Thread.sleep, but does not throw any exceptions, so it is easier for clients to use.
   * Causes the currently executing thread to sleep (temporarily cease execution) for the specified
   * number of milliseconds.
   *
   * @param millis the length of time to sleep in milliseconds
   * @deprecated use SystemPlume.sleep
   */
  @Deprecated // 2021-03-10
  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Concatenates an element, an array, and an element.
   *
   * @param <T> the type of the array elements
   * @param firstElt the first element
   * @param array the array
   * @param lastElt the last elemeent
   * @return a new array containing first element, the array, and the last element, in that order
   * @deprecated use PlumeUtil.concat
   */
  @Deprecated // 2021-03-10
  @SuppressWarnings("unchecked")
  public static <T> T[] concatenate(T firstElt, T[] array, T lastElt) {
    @SuppressWarnings("nullness") // elements are not non-null yet, but will be by return stmt
    T[] result = Arrays.copyOf(array, array.length + 2);
    result[0] = firstElt;
    System.arraycopy(array, 0, result, 1, array.length);
    result[result.length - 1] = lastElt;
    return result;
  }

  /**
   * Return true if the system property is set to "true". Return false if the system property is not
   * set or is set to "false". Otherwise, errs.
   *
   * @param key system property to check
   * @return true if the system property is set to "true". Return false if the system property is
   *     not set or is set to "false". Otherwise, errs.
   * @deprecated use UtilPlume.getBooleanSystemProperty
   */
  @Deprecated // 2021-03-28
  public static boolean getBooleanSystemProperty(String key) {
    return Boolean.parseBoolean(System.getProperty(key, "false"));
  }

  /**
   * Return its boolean value if the system property is set. Return defaultValue if the system
   * property is not set. Errs if the system property is set to a non-boolean value.
   *
   * @param key system property to check
   * @param defaultValue value to use if the property is not set
   * @return the boolean value of {@code key} or {@code defaultValue} if {@code key} is not set
   * @deprecated use UtilPlume.getBooleanSystemProperty
   */
  @Deprecated // 2021-03-28
  public static boolean getBooleanSystemProperty(String key, boolean defaultValue) {
    String value = System.getProperty(key);
    if (value == null) {
      return defaultValue;
    }
    if (value.equals("true")) {
      return true;
    }
    if (value.equals("false")) {
      return false;
    }
    throw new UserError(
        String.format(
            "Value for system property %s should be boolean, but is \"%s\".", key, value));
  }

  /**
   * Concatenates two arrays. Can be invoked varargs-style.
   *
   * @param <T> the type of the array elements
   * @param array1 the first array
   * @param array2 the second array
   * @return a new array containing the contents of the given arrays, in order
   * @deprecated use StringsPlume.concatenate
   */
  @Deprecated // 2021-03-28
  @SuppressWarnings("unchecked")
  public static <T> T[] concatenate(T[] array1, T... array2) {
    @SuppressWarnings("nullness") // elements are not non-null yet, but will be by return stmt
    T[] result = Arrays.copyOf(array1, array1.length + array2.length);
    System.arraycopy(array2, 0, result, array1.length, array2.length);
    return result;
  }

  /**
   * Given an expected number of elements, returns the capacity that should be passed to a HashMap
   * or HashSet constructor, so that the set or map will not resize.
   *
   * @param numElements the maximum expected number of elements in the map or set
   * @return the initial capacity to pass to a HashMap or HashSet constructor
   * @deprecated use CollectionsPlume.mapCapacity
   */
  @Deprecated // 2021-05-05
  public static int mapCapacity(int numElements) {
    // Equivalent to: (int) (numElements / 0.75) + 1
    // where 0.75 is the default load factor.
    return (numElements * 4 / 3) + 1;
  }

  /**
   * Given an expected number of elements, returns the capacity that should be passed to a HashMap
   * or HashSet constructor, so that the set or map will not resize.
   *
   * @param c a collection whose size is the maximum expected number of elements in the map or set
   * @return the initial capacity to pass to a HashMap or HashSet constructor
   * @deprecated use CollectionsPlume.mapCapacity
   */
  @Deprecated // 2021-05-05
  public static int mapCapacity(Collection<?> c) {
    return mapCapacity(c.size());
  }

  /**
   * Given an expected number of elements, returns the capacity that should be passed to a HashMap
   * or HashSet constructor, so that the set or map will not resize.
   *
   * @param m a map whose size is the maximum expected number of elements in the map or set
   * @return the initial capacity to pass to a HashMap or HashSet constructor
   * @deprecated use CollectionsPlume.mapCapacity
   */
  @Deprecated // 2021-05-05
  public static int mapCapacity(Map<?, ?> m) {
    return mapCapacity(m.size());
  }
}
