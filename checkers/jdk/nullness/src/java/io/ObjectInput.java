package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface ObjectInput extends DataInput {
  public abstract java.lang.Object readObject() throws java.lang.ClassNotFoundException, java.io.IOException;
  public abstract int read() throws java.io.IOException;
  public abstract int read(byte[] a1) throws java.io.IOException;
  public abstract int read(byte[] a1, int a2, int a3) throws java.io.IOException;
  public abstract long skip(long a1) throws java.io.IOException;
  public abstract int available() throws java.io.IOException;
  public abstract void close() throws java.io.IOException;
}
