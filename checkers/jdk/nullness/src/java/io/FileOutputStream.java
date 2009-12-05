package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FileOutputStream extends OutputStream {
  public FileOutputStream(java.lang.String a1) throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileOutputStream(java.lang.String a1, boolean a2) throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileOutputStream(java.io.File a1) throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileOutputStream(java.io.File a1, boolean a2) throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileOutputStream(java.io.FileDescriptor a1) { throw new RuntimeException("skeleton method"); }
  public native void write(int a1) throws java.io.IOException;
  public void write(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final java.io.FileDescriptor getFD() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.nio.channels.FileChannel getChannel() { throw new RuntimeException("skeleton method"); }
}
