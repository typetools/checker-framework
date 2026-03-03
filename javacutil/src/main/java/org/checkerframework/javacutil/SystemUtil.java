package org.checkerframework.javacutil;

import com.google.common.base.Splitter;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntVal;

/** This file contains basic utility functions. */
public class SystemUtil {

  /** Do not instantiate. */
  private SystemUtil() {
    throw new Error("Do not instantiate.");
  }

  /** A splitter that splits on periods. The result contains no empty strings. */
  public static final Splitter dotSplitter = Splitter.on('.').omitEmptyStrings();

  /** A splitter that splits on commas. The result contains no empty strings. */
  public static final Splitter commaSplitter = Splitter.on(',').omitEmptyStrings();

  /** A splitter that splits on colons. The result contains no empty strings. */
  public static final Splitter colonSplitter = Splitter.on(':').omitEmptyStrings();

  /** A splitter that splits on {@code File.pathSeparator}. The result contains no empty strings. */
  public static final Splitter pathSeparatorSplitter =
      Splitter.on(File.pathSeparator).omitEmptyStrings();

  /**
   * Like {@code System.getProperty}, but splits on the path separator and never returns null.
   *
   * @param propName a system property name
   * @return the paths in the system property; may be an empty array
   */
  public static final List<String> getPathsProperty(String propName) {
    String propValue = System.getProperty(propName);
    if (propValue == null) {
      return Collections.emptyList();
    } else {
      return pathSeparatorSplitter.splitToList(propValue);
    }
  }

  /**
   * Calls {@code InputStream.available()}, but returns null instead of throwing an IOException.
   *
   * @param is an input stream
   * @return {@code is.available()}, or null if that throws an exception
   */
  public static @Nullable Integer available(InputStream is) {
    try {
      return is.available();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Returns true if the first {@code readLimit} bytes of the input stream consist only of
   * whitespace.
   *
   * @param is an input stream
   * @param readLimit how many bytes to look ahead in the input stream
   * @return null if {@code !is.markSupported()}; otherwise, true if the first {@code readLimit}
   *     characters of the input stream consist only of whitespace
   */
  public static @Nullable Boolean isWhitespaceOnly(InputStream is, int readLimit) {
    if (!is.markSupported()) {
      return null;
    }
    try {
      is.mark(readLimit * 4); // each character is at most 4 bytes, usually much less
      for (int bytesRead = 0; bytesRead < readLimit; bytesRead++) {
        int nextCodePoint = readCodePoint(is);
        if (nextCodePoint == -1) {
          return true;
        } else if (Character.isWhitespace(nextCodePoint)) {
          // do nothing, continue loop
        } else {
          return false;
        }
      }
      return true;
    } finally {
      try {
        is.reset();
      } catch (IOException e) {
        // Do nothing.
      }
    }
  }

  // From https://stackoverflow.com/a/54513347 .
  /**
   * Reads a Unicode code point from an input stream.
   *
   * @param is an input stream
   * @return the Unicode code point for the next character in the input stream
   */
  public static int readCodePoint(InputStream is) {
    try {
      int nextByte = is.read();
      if (nextByte == -1) {
        return -1;
      }
      byte firstByte = (byte) nextByte;
      int byteCount = getByteCount(firstByte);
      if (byteCount == 1) {
        return nextByte;
      }
      byte[] utf8Bytes = new byte[byteCount];
      utf8Bytes[0] = (byte) nextByte;
      for (int i = 1; i < byteCount; i++) { // Get any subsequent bytes for this UTF-8 character.
        nextByte = is.read();
        utf8Bytes[i] = (byte) nextByte;
      }
      int codePoint = new String(utf8Bytes, StandardCharsets.UTF_8).codePointAt(0);
      return codePoint;
    } catch (IOException e) {
      throw new Error("input stream = " + is, e);
    }
  }

  // From https://stackoverflow.com/a/54513347 .
  /**
   * Returns the number of bytes in a UTF-8 character based on the bit pattern of the supplied byte.
   * The only valid values are 1, 2 3 or 4. If the byte has an invalid bit pattern an
   * IllegalArgumentException is thrown.
   *
   * @param b the first byte of a UTF-8 character
   * @return the number of bytes for this UTF-* character
   * @throws IllegalArgumentException if the bit pattern is invalid
   */
  private static @IntVal({1, 2, 3, 4}) int getByteCount(byte b) throws IllegalArgumentException {
    if ((b >= 0)) return 1; // Pattern is 0xxxxxxx.
    if ((b >= (byte) 0b11000000) && (b <= (byte) 0b11011111)) return 2; // Pattern is 110xxxxx.
    if ((b >= (byte) 0b11100000) && (b <= (byte) 0b11101111)) return 3; // Pattern is 1110xxxx.
    if ((b >= (byte) 0b11110000) && (b <= (byte) 0b11110111)) return 4; // Pattern is 11110xxx.
    throw new IllegalArgumentException(); // Invalid first byte for UTF-8 character.
  }

  /** The major version number of the Java runtime (JRE), such as 8, 11, or 17. */
  @SuppressWarnings("deprecation") // remove @SuppressWarnings when getJreVersion() isn't deprecated
  public static final int jreVersion = getJreVersion();

  // Keep in sync with BCELUtil.java (in the bcel-util project).
  /**
   * Returns the major version number from the "java.version" system property, such as 8, 11, or 17.
   *
   * <p>This is different from the version passed to the compiler via {@code --release}; use {@link
   * #getReleaseValue(ProcessingEnvironment)} to get that version.
   *
   * <p>Two possible formats of the "java.version" system property are considered. Up to Java 8,
   * from a version string like `1.8.whatever`, this method extracts 8. Since Java 9, from a version
   * string like `11.0.1`, this method extracts 11.
   *
   * <p>Starting in Java 9, there is the int {@code Runtime.version().feature()}, but that does not
   * exist on JDK 8.
   *
   * @return the major version of the Java runtime
   */
  private static int getJreVersion() {
    String version = System.getProperty("java.version");

    // Up to Java 8, from a version string like "1.8.whatever", extract "8".
    if (version.startsWith("1.")) {
      return Integer.parseInt(version.substring(2, 3));
    }

    // Since Java 9, from a version string like "11.0.1" or "11-ea" or "11u25", extract "11".
    // The format is described at https://openjdk.org/jeps/223 .
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
}
