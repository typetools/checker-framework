package java.io;

import checkers.nullness.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public final class FileDescriptor{
  public final static FileDescriptor in = null;
  public final static FileDescriptor out = null;
  public final static FileDescriptor err = null;
  public FileDescriptor() { throw new RuntimeException("skeleton method"); }
  public boolean valid() { throw new RuntimeException("skeleton method"); }
  public native void sync() throws SyncFailedException;
}
