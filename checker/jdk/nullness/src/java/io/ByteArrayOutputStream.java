package java.io;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public class ByteArrayOutputStream extends OutputStream {
  public ByteArrayOutputStream() { throw new RuntimeException("skeleton method"); }
  public ByteArrayOutputStream(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void write(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void write(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized void writeTo(OutputStream a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void reset() { throw new RuntimeException("skeleton method"); }
  public synchronized byte[] toByteArray() { throw new RuntimeException("skeleton method"); }
  @Pure public synchronized int size() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public synchronized String toString() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public synchronized String toString(String a1) throws UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public synchronized String toString(int a1) { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
