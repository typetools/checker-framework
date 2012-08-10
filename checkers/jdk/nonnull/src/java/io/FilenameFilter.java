package java.io;

import checkers.nonnull.quals.Nullable;


public abstract interface FilenameFilter{
  public abstract boolean accept(File a1, String a2);
}
