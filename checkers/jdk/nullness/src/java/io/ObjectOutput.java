package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface ObjectOutput extends DataOutput {
  public abstract void writeObject(java.lang.Object a1) throws java.io.IOException;
  public abstract void write(int a1) throws java.io.IOException;
  public abstract void write(byte[] a1) throws java.io.IOException;
  public abstract void write(byte[] a1, int a2, int a3) throws java.io.IOException;
  public abstract void flush() throws java.io.IOException;
  public abstract void close() throws java.io.IOException;
}
