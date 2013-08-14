package java.lang;

import dataflow.quals.Pure;
import dataflow.quals.SideEffectFree;
import checkers.nullness.quals.Nullable;


public abstract interface CharSequence{
  @Pure public abstract int length();
  public abstract char charAt(int a1);
  public abstract CharSequence subSequence(int a1, int a2);
  @SideEffectFree public abstract String toString();
}
