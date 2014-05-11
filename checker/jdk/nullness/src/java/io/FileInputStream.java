package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

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
