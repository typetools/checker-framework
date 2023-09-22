import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class SimpleCaseGuard {

  @Nullable String field;

  void test2(Object obj, boolean b) {
    switch (obj) {
      case String s when field != null -> {
        @NonNull String z = field;
      }
      case String s -> {
        // :: error: (assignment)
        @NonNull String z = field;
      }
      default -> {
        // :: error: (assignment)
        @NonNull String z = field;
      }
    }
  }

}
