package open.falsepos;

import java.util.Collection;
import java.util.stream.Stream;

// Use the Nullness Checker. This has something to do with @PolyNull.
public class Issue6739 {

  public <T> Collection<T> clear(Class<T> type) {
    return Stream.of("abc").map(type::cast).toList();
  }

  public <T> Collection<T> clear2(Class<T> type) {
    return Stream.of("abc").map(value -> type.cast(value)).toList();
  }
}
