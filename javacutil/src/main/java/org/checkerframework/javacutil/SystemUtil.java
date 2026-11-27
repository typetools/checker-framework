package org.checkerframework.javacutil;

import com.google.common.base.Splitter;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;

/** This file contains basic utility functions. */
public final class SystemUtil {

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
   * @return the paths in the system property; might be an empty list
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

  /** The major version number of the Java runtime (JRE), such as 17, 21, or 25. */
  public static final int jreVersion = Runtime.version().feature();

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
