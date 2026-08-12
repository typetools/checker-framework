import java.util.List;
import java.util.stream.Collector;

// Check for crashes.
@SuppressWarnings("all")
public class Issue7683 {
  static <E> Collector<E, ?, List<E>> toList() {
    throw new UnsupportedOperationException();
  }

  Object convert(Object value) {
    if (value instanceof List) {
      return ((List) value).stream().map(item -> convert(item)).collect(toList());
    }

    return value;
  }
}
