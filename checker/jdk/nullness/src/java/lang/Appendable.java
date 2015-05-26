package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface Appendable{
  Appendable append(@Nullable CharSequence a1) throws java.io.IOException;
  Appendable append(@Nullable CharSequence a1, int a2, int a3) throws java.io.IOException;
  Appendable append(char a1) throws java.io.IOException;
}
