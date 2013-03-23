package java.io;

import checkers.nullness.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public abstract interface Flushable{
  public abstract void flush() throws IOException;
}
