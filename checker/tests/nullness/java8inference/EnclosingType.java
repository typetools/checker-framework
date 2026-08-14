// A type variable may be mentioned only in the enclosing type of an inner class type, as in
// `Outer<T>.Inner`.  Such a type is not a proper type, so inference must treat it as one that
// mentions an inference variable.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EnclosingType {

  static class Outer<T> {
    class Inner {}
  }

  static <T> Outer<T>.Inner make(T t) {
    throw new RuntimeException();
  }

  static <T> Outer<T>.Inner id(Outer<T>.Inner p) {
    return p;
  }

  static <T> T fromInner(Outer<T>.Inner p) {
    throw new RuntimeException();
  }

  static <T> void twoUses(Outer<T>.Inner p, T t) {}

  void useMake(@Nullable String ns, @NonNull String nn) {
    Outer<@NonNull String>.Inner a = make(ns);
    Outer<@Nullable String>.Inner b = make(nn);
  }

  void useOther(Outer<@Nullable String>.Inner inner, @NonNull String nn) {
    Outer<@NonNull String>.Inner c = id(inner);
    Object o = fromInner(inner);
    twoUses(inner, nn);
  }
}
