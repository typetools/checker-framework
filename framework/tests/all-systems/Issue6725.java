package typearginfer;

import java.util.stream.Stream;

public class Issue6725 {
  static <T> void prefix(Stream<? extends Iterable<? extends T>> iterables) {
    iterables.map(Iterable::iterator);
  }
}
