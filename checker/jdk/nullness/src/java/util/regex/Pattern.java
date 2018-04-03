package java.util.regex;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public final class Pattern implements java.io.Serializable{
    private static final long serialVersionUID = 0L;
  protected Pattern() {}
  public final static int UNIX_LINES = 1;
  public final static int CASE_INSENSITIVE = 2;
  public final static int COMMENTS = 4;
  public final static int MULTILINE = 8;
  public final static int LITERAL = 16;
  public final static int DOTALL = 32;
  public final static int UNICODE_CASE = 64;
  public final static int CANON_EQ = 128;
  @Pure  // pure wrt equals() but not ==
  public static Pattern compile(String a1) { throw new RuntimeException("skeleton method"); }
  @Pure  // pure wrt equals() but not ==
  public static Pattern compile(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String pattern() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  public Matcher matcher(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public int flags() { throw new RuntimeException("skeleton method"); }
  public static boolean matches(String a1, CharSequence a2) { throw new RuntimeException("skeleton method"); }
  public String[] split(CharSequence a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String[] split(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  @Pure  // pure wrt equals() but not ==
  public static String quote(String a1) { throw new RuntimeException("skeleton method"); }
}
