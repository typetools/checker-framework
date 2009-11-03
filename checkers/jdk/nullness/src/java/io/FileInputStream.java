package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FileInputStream extends InputStream {
  public FileInputStream(java.lang.String a1) throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileInputStream(java.io.File a1) throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileInputStream(java.io.FileDescriptor a1) { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final java.io.FileDescriptor getFD() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.nio.channels.FileChannel getChannel() { throw new RuntimeException("skeleton method"); }

  public native int available() throws java.io.IOException;
  public native int read() throws java.io.IOException;
  public native long skip(long a1) throws java.io.IOException;

}
