package java.io;

import checkers.nonnull.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nonnull.quals.NonNull.class)

public abstract interface Closeable{
  public abstract void close() throws IOException;
}
