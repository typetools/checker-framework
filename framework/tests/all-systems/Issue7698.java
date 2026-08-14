// Hard crash
public abstract class Issue7698 {
  interface AsyncWork<R> {
    Promise<R> apply(Promise<Iterable<Box<Long>>> promise) throws Exception;
  }

  interface Box<T> {
    T value();
  }

  interface Promise<T> {
    <U> Promise<U> then(Function<? super T, ? extends U> function);
  }

  interface MyStream<E> {
    <R> MyStream<R> map(Function<? super E, ? extends R> mapper);
  }

  interface Function<F, T> {
    T apply(F f);
  }

  abstract <R> Promise<R> run(AsyncWork<R> work);

  abstract <T> MyStream<T> myStream(Iterable<T> iterable);

  Promise<MyStream<Long>> main() {
    return run(promise -> promise.then(results -> myStream(results).map(result -> result.value())));
  }
}
