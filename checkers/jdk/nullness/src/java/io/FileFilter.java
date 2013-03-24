package java.io;

import checkers.nullness.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public abstract interface FileFilter{
  public abstract boolean accept(File a1);
}
