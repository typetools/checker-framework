package java.io;

import checkers.nullness.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public abstract interface Closeable{
  public abstract void close() throws IOException;
}
