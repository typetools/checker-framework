package java.io;

import checkers.nullness.quals.Nullable;


public abstract interface FileFilter{
  public abstract boolean accept(File a1);
}
