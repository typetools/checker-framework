import org.checkerframework.checker.nullness.qual.NonNull;

public class Issue5006 {

  static class C<T> {
    T get() {
      throw new RuntimeException("");
    }
  }

  interface X {
    C<? extends Object> get();
  }

  interface Y extends X {
    @Override
    // :: error: (override.return)
    C<? super Object> get();
  }

  interface Z extends Y {
    @Override
    C<Object> get();
  }

  interface Q {
    C<? super Object> get();
  }

  interface R extends Q {
    @Override
    C<@NonNull Object> get();
  }
}
