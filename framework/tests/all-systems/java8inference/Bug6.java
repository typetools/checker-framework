package inference.guava;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SuppressWarnings("all") // Just check for crashes.
public class Bug6 {
  public static <Q> Iterable<Q> method(final Iterable<Q> iterable) {
    return new Iterable<Q>() {
      @Override
      public Iterator<Q> iterator() {
        throw new RuntimeException();
      }

      @Override
      public void forEach(Consumer<? super Q> action) {
        throw new RuntimeException();
      }

      @Override
      public Spliterator<Q> spliterator() {
        return Stream.generate(() -> iterable).flatMap(Bug6::stream).spliterator();
      }
    };
  }

  public static <Z> Stream<Z> stream(Iterable<Z> iterable) {
    throw new RuntimeException();
  }
}
