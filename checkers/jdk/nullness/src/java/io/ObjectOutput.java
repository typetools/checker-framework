package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface ObjectOutput extends DataOutput {
  public abstract void writeObject(Object a1) throws IOException;
  public abstract void write(int a1) throws IOException;
  public abstract void write(byte[] a1) throws IOException;
  public abstract void write(byte[] a1, int a2, int a3) throws IOException;
  public abstract void flush() throws IOException;
  public abstract void close() throws IOException;
}
