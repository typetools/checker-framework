package java.io;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.lock.qual.*;



public class CharArrayWriter extends Writer {
  public CharArrayWriter() { throw new RuntimeException("skeleton method"); }
  public CharArrayWriter(int a1) { throw new RuntimeException("skeleton method"); }
  public void write(int a1) { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void write(String a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void writeTo(Writer a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public CharArrayWriter append(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public CharArrayWriter append(CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public CharArrayWriter append(char a1) { throw new RuntimeException("skeleton method"); }
  public void reset() { throw new RuntimeException("skeleton method"); }
  public char[] toCharArray() { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied CharArrayWriter this) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied CharArrayWriter this) { throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
  public void close() { throw new RuntimeException("skeleton method"); }
}
