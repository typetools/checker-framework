package java.io;

import checkers.nonnull.quals.Nullable;


public abstract interface Closeable{
  public abstract void close() throws IOException;
}
