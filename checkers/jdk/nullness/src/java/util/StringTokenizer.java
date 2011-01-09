package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class StringTokenizer implements Enumeration<@NonNull Object> {
  public StringTokenizer(String a1, @Nullable String a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  public StringTokenizer(String a1, @Nullable String a2) { throw new RuntimeException("skeleton method"); }
  public StringTokenizer(String a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasMoreTokens() { throw new RuntimeException("skeleton method"); }
  public String nextToken() { throw new RuntimeException("skeleton method"); }
  public String nextToken(String a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasMoreElements() { throw new RuntimeException("skeleton method"); }
  public Object nextElement() { throw new RuntimeException("skeleton method"); }
  public int countTokens() { throw new RuntimeException("skeleton method"); }
}
