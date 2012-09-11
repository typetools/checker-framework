package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public abstract interface FileFilter{
  public abstract boolean accept(File a1);
}
