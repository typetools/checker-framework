package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public abstract interface FilenameFilter{
  public abstract boolean accept(File a1, String a2);
}
