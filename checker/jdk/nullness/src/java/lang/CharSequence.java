package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;


public abstract interface CharSequence{
  @Pure public abstract int length();
  public abstract char charAt(int a1);
  public abstract CharSequence subSequence(int a1, int a2);
  @SideEffectFree public abstract String toString();
}
