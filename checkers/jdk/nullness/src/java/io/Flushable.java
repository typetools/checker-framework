package java.io;

import checkers.nullness.quals.Nullable;


public abstract interface Flushable{
  public abstract void flush() throws IOException;
}
