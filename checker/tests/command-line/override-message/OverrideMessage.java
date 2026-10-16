// A diagnostic about an override names the type variables that the overriding method declares,
// rather than the corresponding type variables of the overridden method.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OverrideMessage {
  static class Super {
    <T extends @Nullable Object> void f(T p) {}
  }

  static class Sub extends Super {
    @Override
    <S extends @Nullable Object> void f(@NonNull S p) {}
  }
}
