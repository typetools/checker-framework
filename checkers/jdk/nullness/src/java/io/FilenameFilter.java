package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface FilenameFilter{
  public abstract boolean accept(java.io.File a1, java.lang.String a2);
}
