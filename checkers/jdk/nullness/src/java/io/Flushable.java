package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface Flushable{
  public abstract void flush() throws IOException;
}
