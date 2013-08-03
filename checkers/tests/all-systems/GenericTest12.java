import checkers.nullness.quals.Nullable;

// Test case from Issue 142
abstract class GenericTest12 {
  interface Task<V extends @Nullable Object> {}

  abstract <V extends @Nullable Object> Task<V> create(Runnable runnable, V result);

  void submit(Runnable runnable) {
    Task<Void> task = create(runnable, null);
  }
}
