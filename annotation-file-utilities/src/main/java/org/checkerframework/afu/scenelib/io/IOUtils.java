package org.checkerframework.afu.scenelib.io;

/** {@code IOUtils} has some static methods useful to scene I/O code. */
class IOUtils {
  private IOUtils() {}

  static String packagePart(String className) {
    int lastdot = className.lastIndexOf('.');
    return (lastdot == -1) ? "" : className.substring(0, lastdot);
  }

  static String basenamePart(String className) {
    int lastdot = className.lastIndexOf('.');
    return (lastdot == -1) ? className : className.substring(lastdot + 1);
  }
}
