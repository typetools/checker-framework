package java.io;

import checkers.nullness.quals.Nullable;


public abstract interface Closeable{
  public abstract void close() throws IOException;
}
