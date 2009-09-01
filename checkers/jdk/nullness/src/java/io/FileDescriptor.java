package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class FileDescriptor{
  public final static java.io.FileDescriptor in;
  public final static java.io.FileDescriptor out;
  public final static java.io.FileDescriptor err;
  public FileDescriptor() { throw new RuntimeException("skeleton method"); }
  public boolean valid() { throw new RuntimeException("skeleton method"); }
}
