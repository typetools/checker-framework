package java.io;

import org.checkerframework.checker.lock.qual.*;



public class ByteArrayOutputStream extends OutputStream {
  public ByteArrayOutputStream() { throw new RuntimeException("skeleton method"); }
  public ByteArrayOutputStream(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void write(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void write(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized void writeTo(OutputStream a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void reset() { throw new RuntimeException("skeleton method"); }
  public synchronized byte[] toByteArray() { throw new RuntimeException("skeleton method"); }
   public synchronized int size(@GuardSatisfied ByteArrayOutputStream this) { throw new RuntimeException("skeleton method"); }
   public synchronized String toString(@GuardSatisfied ByteArrayOutputStream this) { throw new RuntimeException("skeleton method"); }
   public synchronized String toString(@GuardSatisfied ByteArrayOutputStream this,String a1) throws UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
   public synchronized String toString(@GuardSatisfied ByteArrayOutputStream this,int a1) { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
