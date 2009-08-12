package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class InputStreamReader extends Reader {
  public InputStreamReader(java.io.InputStream a1) { throw new RuntimeException("skeleton method"); }
  public InputStreamReader(java.io.InputStream a1, java.lang.String a2) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public InputStreamReader(java.io.InputStream a1, java.nio.charset.Charset a2) { throw new RuntimeException("skeleton method"); }
  public InputStreamReader(java.io.InputStream a1, java.nio.charset.CharsetDecoder a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getEncoding() { throw new RuntimeException("skeleton method"); }
  public int read() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int read(char[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public boolean ready() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
