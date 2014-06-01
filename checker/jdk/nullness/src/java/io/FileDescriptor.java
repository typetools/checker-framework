package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public final class FileDescriptor{
  public final static FileDescriptor in = null;
  public final static FileDescriptor out = null;
  public final static FileDescriptor err = null;
  public FileDescriptor() { throw new RuntimeException("skeleton method"); }
  public boolean valid() { throw new RuntimeException("skeleton method"); }
  public native void sync() throws SyncFailedException;
}
