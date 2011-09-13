package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class InputStreamReader extends Reader {
  public InputStreamReader(InputStream a1) { throw new RuntimeException("skeleton method"); }
  public InputStreamReader(InputStream a1, String a2) throws UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public InputStreamReader(InputStream a1, java.nio.charset.Charset a2) { throw new RuntimeException("skeleton method"); }
  public InputStreamReader(InputStream a1, java.nio.charset.CharsetDecoder a2) { throw new RuntimeException("skeleton method"); }
  public @Nullable String getEncoding() { throw new RuntimeException("skeleton method"); }
  public int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(char[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean ready() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
