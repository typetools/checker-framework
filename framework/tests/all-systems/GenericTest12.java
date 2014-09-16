// Test case from Issue 142
abstract class GenericTest12 {
  interface Task<V> {}

  abstract <V> Task<V> create(Runnable runnable, V result);

  void submit(Runnable runnable) {
    Task<Void> task = create(runnable, null);
  }
}
