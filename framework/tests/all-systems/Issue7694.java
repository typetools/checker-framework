import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@SuppressWarnings({
  "optional.as.element.type",
  "optional.null.comparison",
  "nulltest.redundant"
}) // true positives
public class Issue7694 {
  static class Foo {}

  interface Promise<T> {
    <U> Promise<U> then(Function<? super T, ? extends U> function);
  }

  static <O, C> Promise<C> make(
      Supplier<Promise<O>> function, Collector<? super O, ?, C> collector) {
    throw new UnsupportedOperationException();
  }

  static <E> Collector<E, ?, List<E>> toImmutableList() {
    throw new UnsupportedOperationException();
  }

  Promise<?> run(Promise<Optional<Foo>> promise) {
    return make(
        () ->
            promise.then(
                o -> {
                  if (o != null) {
                    return o;
                  }
                  return Optional.empty();
                }),
        toImmutableList());
  }
}
