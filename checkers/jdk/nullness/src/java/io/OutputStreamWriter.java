package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class OutputStreamWriter extends Writer {
  public OutputStreamWriter(OutputStream a1, String a2) throws UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public OutputStreamWriter(OutputStream a1) { throw new RuntimeException("skeleton method"); }
  public OutputStreamWriter(OutputStream a1, java.nio.charset.Charset a2) { throw new RuntimeException("skeleton method"); }
  public OutputStreamWriter(OutputStream a1, java.nio.charset.CharsetEncoder a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable String getEncoding() { throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(String a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
