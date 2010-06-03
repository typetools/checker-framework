package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Console implements Flushable {
  protected Console() {}
  public PrintWriter writer() { throw new RuntimeException("skeleton method"); }
  public Reader reader() { throw new RuntimeException("skeleton method"); }
  // TODO: format() and printf() may accept null a2's based on the format of a1
  public Console format(String a1, @Nullable Object ... a2) { throw new RuntimeException("skeleton method"); }
  public Console printf(String a1, @Nullable Object ... a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable String readLine(String a1, Object... a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable String readLine() { throw new RuntimeException("skeleton method"); }
  public char @Nullable [] readPassword(String a1, Object... a2) { throw new RuntimeException("skeleton method"); }
    public char @Nullable [] readPassword() { throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
}
