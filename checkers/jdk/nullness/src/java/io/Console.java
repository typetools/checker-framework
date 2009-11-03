package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Console implements Flushable {
  protected Console() {}
  public java.io.PrintWriter writer() { throw new RuntimeException("skeleton method"); }
  public java.io.Reader reader() { throw new RuntimeException("skeleton method"); }
  // TODO: format() and printf() may accept null a2's based on the format of a1
  public java.io.Console format(java.lang.String a1, @Nullable java.lang.Object ... a2) { throw new RuntimeException("skeleton method"); }
  public java.io.Console printf(java.lang.String a1, @Nullable java.lang.Object ... a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String readLine(java.lang.String a1, java.lang.Object... a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String readLine() { throw new RuntimeException("skeleton method"); }
  public char @Nullable [] readPassword(java.lang.String a1, java.lang.Object... a2) { throw new RuntimeException("skeleton method"); }
    public char @Nullable [] readPassword() { throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
}
