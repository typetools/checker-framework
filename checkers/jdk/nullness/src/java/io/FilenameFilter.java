package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface FilenameFilter{
  public abstract boolean accept(File a1, String a2);
}
