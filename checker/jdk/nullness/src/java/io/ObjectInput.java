package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;


public abstract interface ObjectInput extends DataInput {
  public abstract Object readObject() throws ClassNotFoundException, IOException;
  public abstract int read() throws IOException;
  public abstract int read(byte[] a1) throws IOException;
  public abstract int read(byte[] a1, int a2, int a3) throws IOException;
  public abstract long skip(long a1) throws IOException;
  public abstract int available() throws IOException;
  public abstract void close() throws IOException;
}
