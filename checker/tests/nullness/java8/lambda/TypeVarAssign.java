import org.checkerframework.checker.nullness.qual.*;

interface Fn<T> {
  T func(T t);
}

class TestAssign {
  <M extends @NonNull Object> void foo(Fn<M> f) {}

  void context() {
    foo((@NonNull String s) -> s);
  }
}
