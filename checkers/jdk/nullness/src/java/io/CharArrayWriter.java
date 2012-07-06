package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class CharArrayWriter extends Writer {
  public CharArrayWriter() { throw new RuntimeException("skeleton method"); }
  public CharArrayWriter(int a1) { throw new RuntimeException("skeleton method"); }
  public void write(int a1) { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void write(String a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void writeTo(Writer a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public CharArrayWriter append(@Nullable CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public CharArrayWriter append(@Nullable CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public CharArrayWriter append(char a1) { throw new RuntimeException("skeleton method"); }
  public void reset() { throw new RuntimeException("skeleton method"); }
  public char[] toCharArray() { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
  public void close() { throw new RuntimeException("skeleton method"); }
}
