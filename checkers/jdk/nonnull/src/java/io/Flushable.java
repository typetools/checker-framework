package java.io;

import checkers.nonnull.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nonnull.quals.NonNull.class)

public abstract interface Flushable{
  public abstract void flush() throws IOException;
}
