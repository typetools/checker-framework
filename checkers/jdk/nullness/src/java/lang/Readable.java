package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface Readable{
  public abstract int read(java.nio.CharBuffer a1) throws java.io.IOException;
}
