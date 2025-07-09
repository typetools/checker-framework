package org.checkerframework.afu.scenelib.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** Handle javac class {@code CommandLine} under all versions of the JDK. */
public class CommandLineUtils {
  /**
   * Calls {@code CommandLine.parse}, handling both JDK 8-11 (where it takes and returns arrays) and
   * later JDKs, where it takes and returns a list.
   *
   * @param args the command line
   * @return the result of calling {@code CommandLine.parse}
   */
  public static String[] parseCommandLine(String[] args) {
    try {
      Class<?> clazz;
      try {
        clazz = Class.forName("com.sun.tools.javac.main.CommandLine");
      } catch (ClassNotFoundException e) {
        clazz = Class.forName("jdk.internal.opt.CommandLine");
      }
      try {
        Method method = clazz.getMethod("parse", List.class);
        return ((List<?>) method.invoke(null, Arrays.asList(args))).toArray(new String[0]);
      } catch (NoSuchMethodException e) {
        Method method = clazz.getMethod("parse", String[].class);
        return (String[]) method.invoke(null, (Object) args);
      }
    } catch (IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException
        | ClassNotFoundException e) {
      throw new Error("Cannot access CommandLine.parse", e);
    }
  }
}
