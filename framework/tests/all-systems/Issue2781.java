// Test case for Issue 2781:
// https://github.com/typetools/checker-framework/issues/2781

import java.util.function.Function;
import java.util.stream.Stream;

public class Issue2781 {
  class Wrapper<T> {
    Wrapper(T t) {}
  }

  Stream<Wrapper<Function<String, String>>> getStreamOfWrappedFunctions1() {
    // inferred type in new
    return Stream.<Wrapper<Function<String, String>>>of(new Wrapper<>(e -> e));
  }

  Stream<Wrapper<Function<String, String>>> getStreamOfWrappedFunctions2() {
    // explicit type in new
    return Stream.<Wrapper<Function<String, String>>>of(
        new Wrapper<Function<String, String>>(e -> e));
  }
}
