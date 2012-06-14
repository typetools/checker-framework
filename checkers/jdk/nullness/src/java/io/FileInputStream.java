package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FileInputStream extends InputStream {
  public FileInputStream(String a1) throws FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileInputStream(File a1) throws FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileInputStream(FileDescriptor a1) { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
  public final FileDescriptor getFD() throws IOException { throw new RuntimeException("skeleton method"); }
  public java.nio.channels.FileChannel getChannel() { throw new RuntimeException("skeleton method"); }

  public native int available() throws IOException;
  public native int read() throws IOException;
  public native long skip(long a1) throws IOException;

}
