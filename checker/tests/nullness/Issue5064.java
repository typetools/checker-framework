// Test case for https://github.com/typetools/checker-framework/issues/5064

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue5064 {
  void m() {
    List<? extends @NonNull String> list = new ArrayList<>(Arrays.asList("bla"));
    Stream<? extends @NonNull String> stream = list.stream();
    Predicate<@Nullable Object> filter = (obj) -> true;
    Predicate<? super @NonNull String> filter2 = filter;
    long count = stream.filter(filter2).count();
  }
}
