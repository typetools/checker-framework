package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public interface CharSequence{
  @Pure int length();
  char charAt(int a1);
  CharSequence subSequence(int a1, int a2);
  @SideEffectFree String toString();
}
