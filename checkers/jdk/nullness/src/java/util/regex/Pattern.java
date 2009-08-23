package java.util.regex;

import checkers.nullness.quals.*;

public final class Pattern{
  public final static int UNIX_LINES = 1;
  public final static int CASE_INSENSITIVE = 2;
  public final static int COMMENTS = 4;
  public final static int MULTILINE = 8;
  public final static int LITERAL = 16;
  public final static int DOTALL = 32;
  public final static int UNICODE_CASE = 64;
  public final static int CANON_EQ = 128;
  public static @NonNull java.util.regex.Pattern compile(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static @NonNull java.util.regex.Pattern compile(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String pattern() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.Matcher matcher(java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public int flags() { throw new RuntimeException("skeleton method"); }
  public static boolean matches(java.lang.String a1, java.lang.CharSequence a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String[] split(java.lang.CharSequence a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String[] split(java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String quote(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
}
