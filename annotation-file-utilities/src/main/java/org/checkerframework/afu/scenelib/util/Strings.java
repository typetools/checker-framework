package org.checkerframework.afu.scenelib.util;

/** {@link Strings} provides useful static methods related to strings. */
public abstract class Strings {
  private Strings() {}

  /**
   * Returns the given string, escaped and quoted according to Java conventions. Currently, only
   * newlines, backslashes, tabs, and single and double quotes are escaped. Perhaps nonprinting
   * characters should also be escaped somehow.
   */
  public static String escape(String in) {
    StringBuilder out = new StringBuilder("\"");
    for (int pos = 0; pos < in.length(); pos++) {
      switch (in.charAt(pos)) {
        case '\n':
          out.append("\\n");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '\'':
          out.append("\\\'");
          break;
        case '\"':
          out.append("\\\"");
          break;
        default:
          out.append(in.charAt(pos));
      }
    }
    out.append('\"');
    return out.toString();
  }
}
