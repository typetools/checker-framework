package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FileOutputStream extends OutputStream {
  public FileOutputStream(String a1) throws FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileOutputStream(String a1, boolean a2) throws FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileOutputStream(File a1) throws FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileOutputStream(File a1, boolean a2) throws FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public FileOutputStream(FileDescriptor a1) { throw new RuntimeException("skeleton method"); }
  public native void write(int a1) throws IOException;
  public void write(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
  public final FileDescriptor getFD() throws IOException { throw new RuntimeException("skeleton method"); }
  public java.nio.channels.FileChannel getChannel() { throw new RuntimeException("skeleton method"); }
}
