package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public abstract interface Closeable{
  public abstract void close() throws IOException;
}
