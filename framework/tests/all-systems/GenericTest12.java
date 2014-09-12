// Test case from Issue 142
abstract class GenericTest12 {
  interface Task<V> {}

  abstract <M> Task<M> create(Runnable runnable, M result);

  void submit(Runnable runnable) {
    Task<Void> task = create(runnable, null);
  }
}
