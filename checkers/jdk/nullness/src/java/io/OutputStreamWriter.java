package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class OutputStreamWriter extends Writer {
  public OutputStreamWriter(java.io.OutputStream a1, java.lang.String a2) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public OutputStreamWriter(java.io.OutputStream a1) { throw new RuntimeException("skeleton method"); }
  public OutputStreamWriter(java.io.OutputStream a1, java.nio.charset.Charset a2) { throw new RuntimeException("skeleton method"); }
  public OutputStreamWriter(java.io.OutputStream a1, java.nio.charset.CharsetEncoder a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getEncoding() { throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(java.lang.String a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
