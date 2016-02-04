package java.io;

import org.checkerframework.checker.lock.qual.GuardSatisfied;

// Note that the @GuardSatisfied is for locks that are external
// to the implementation class.
public class FilterOutputStream extends OutputStream {
  protected FilterOutputStream() {}
  public FilterOutputStream(OutputStream a1) { super(); throw new RuntimeException("skeleton method"); }
  public void write(@GuardSatisfied FilterOutputStream this, int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(@GuardSatisfied FilterOutputStream this, byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(@GuardSatisfied FilterOutputStream this, byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void flush(@GuardSatisfied FilterOutputStream this) throws IOException { throw new RuntimeException("skeleton method"); }
  public void close(@GuardSatisfied FilterOutputStream this) throws IOException { throw new RuntimeException("skeleton method"); }
}
