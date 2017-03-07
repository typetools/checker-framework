package java.io;

public final class FileDescriptor{
  public final static FileDescriptor in = null;
  public final static FileDescriptor out = null;
  public final static FileDescriptor err = null;
  public FileDescriptor() { throw new RuntimeException("skeleton method"); }
  public boolean valid() { throw new RuntimeException("skeleton method"); }
  public native void sync() throws SyncFailedException;
  synchronized void attach(Closeable c) { throw new RuntimeException("skeleton method"); }
  synchronized void closeAll(Closeable releaser) throws IOException { throw new RuntimeException("skeleton method"); }
}
