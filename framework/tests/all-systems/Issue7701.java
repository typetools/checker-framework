@SuppressWarnings("all") // Just check for crashes.
public class Issue7701 {
  interface Iterator<E> {}

  interface CheckedIterator<T, X extends Exception> {}

  interface CheckedFunction<F, T, X extends Exception> {
    T apply(F input) throws X;
  }

  <F, T, X extends Exception> CheckedIterator<T, X> transform(
      Iterator<? extends F> fromIterator,
      CheckedFunction<? super F, ? extends T, ? extends X> function) {
    throw new UnsupportedOperationException();
  }

  interface Foo {
    Bar toBar();
  }

  interface Bar {}

  CheckedIterator<Bar, ?> run(Iterator<Foo> rows) {
    return transform(rows, row -> row.toBar());
  }
}
