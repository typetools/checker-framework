package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class StringTokenizer implements java.util.Enumeration<@NonNull java.lang.Object> {
  public StringTokenizer(java.lang.String a1, @Nullable java.lang.String a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  public StringTokenizer(java.lang.String a1, @Nullable java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public StringTokenizer(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasMoreTokens() { throw new RuntimeException("skeleton method"); }
  public java.lang.String nextToken() { throw new RuntimeException("skeleton method"); }
  public java.lang.String nextToken(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasMoreElements() { throw new RuntimeException("skeleton method"); }
  public java.lang.Object nextElement() { throw new RuntimeException("skeleton method"); }
  public int countTokens() { throw new RuntimeException("skeleton method"); }
}
