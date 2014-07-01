package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public final class Console implements Flushable {
  protected Console() {}
  public PrintWriter writer() { throw new RuntimeException("skeleton method"); }
  public Reader reader() { throw new RuntimeException("skeleton method"); }
  public Console format(String a1, @Nullable Object ... a2) { throw new RuntimeException("skeleton method"); }
  public Console printf(String a1, @Nullable Object ... a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable String readLine(String a1, @Nullable Object... a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable String readLine() { throw new RuntimeException("skeleton method"); }
  public char @Nullable [] readPassword(String a1, @Nullable Object... a2) { throw new RuntimeException("skeleton method"); }
    public char @Nullable [] readPassword() { throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
}
