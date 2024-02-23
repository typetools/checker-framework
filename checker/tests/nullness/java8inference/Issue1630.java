import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1630 {
  static @Nullable String toString(Object o) {
    return null;
  }

  public static List<@Nullable String> f(@Nullable List<Integer> xs) {
    return xs != null
        ? xs.stream().map(Issue1630::toString).filter(Objects::nonNull).collect(Collectors.toList())
        : Collections.emptyList();
  }

  public static List<String> f2(@Nullable List<Integer> xs) {
    return xs != null
        // TODO: we could refine the type of filter is the postconditions of the predicate
        // is @EnsuresNonNull("#1").
        // :: error: (type.arguments.not.inferred)
        ? xs.stream().map(Issue1630::toString).filter(Objects::nonNull).collect(Collectors.toList())
        : Collections.emptyList();
  }
}
