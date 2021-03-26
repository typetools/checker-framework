import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.PolyNull;

// Test case for Issue 602
// https://github.com/typetools/checker-framework/issues/602
// @skip-test
public class Issue602 {
  @PolyNull String id(@PolyNull String o) {
    return o;
  }

  void loop(boolean condition) {
    @NonNull String notNull = "hello";
    String nullable = "";
    while (condition) {
      // :: error: (assignment.type.incompatible)
      notNull = nullable;
      // :: error: (assignment.type.incompatible)
      notNull = id(nullable);
      nullable = null;
    }
  }
}
