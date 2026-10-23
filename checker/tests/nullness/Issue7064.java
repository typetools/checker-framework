// Test case for https://github.com/typetools/checker-framework/issues/7064

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

public class Issue7064 {
  static <T, C extends Collection<T>> C toCollection(List<T> list, Supplier<C> supplier) {
    throw new Error();
  }

  static <T> Comparator<T> comparing(Function<T, String> keyExtractor) {
    throw new Error();
  }

  static <R> void map(Function<List<String>, R> mapper) {}

  static String key(String s) {
    return s;
  }

  void use() {
    map(list -> toCollection(list, () -> new TreeSet<>(comparing(Issue7064::key))));
  }
}
