package java.io;

import checkers.nonnull.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nonnull.quals.NonNull.class)

public abstract interface FileFilter{
  public abstract boolean accept(File a1);
}
