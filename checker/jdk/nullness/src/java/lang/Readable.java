package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;


public abstract interface Readable{
  public abstract int read(java.nio.CharBuffer a1) throws java.io.IOException;
}
