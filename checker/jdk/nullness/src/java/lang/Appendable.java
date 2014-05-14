package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract interface Appendable{
  public abstract Appendable append(@Nullable CharSequence a1) throws java.io.IOException;
  public abstract Appendable append(@Nullable CharSequence a1, int a2, int a3) throws java.io.IOException;
  public abstract Appendable append(char a1) throws java.io.IOException;
}
