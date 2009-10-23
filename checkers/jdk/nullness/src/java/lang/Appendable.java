package java.lang;

import checkers.nullness.quals.*;

public abstract interface Appendable{
  public abstract java.lang.Appendable append(@Nullable java.lang.CharSequence a1) throws java.io.IOException;
  public abstract java.lang.Appendable append(@Nullable java.lang.CharSequence a1, int a2, int a3) throws java.io.IOException;
  public abstract java.lang.Appendable append(char a1) throws java.io.IOException;
}
