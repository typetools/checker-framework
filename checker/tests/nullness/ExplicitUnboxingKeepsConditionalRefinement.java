import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

// An explicit call to booleanValue() keeps the facts that hold when its receiver is true or false.
public class ExplicitUnboxingKeepsConditionalRefinement {
  @EnsuresNonNullIf(expression = "#1", result = true)
  static Boolean isNN(@Nullable Object o) {
    return o != null;
  }

  @EnsuresNonNullIf(expression = "#1", result = false)
  static Boolean isNull(@Nullable Object o) {
    return o == null;
  }

  void ifThen(@Nullable Object o) {
    if (isNN(o).booleanValue()) {
      o.hashCode();
    }
  }

  void ifElse(@Nullable Object o) {
    if (isNull(o).booleanValue()) {
      // :: error: [dereference.of.nullable]
      o.hashCode();
    } else {
      o.hashCode();
    }
  }

  void not(@Nullable Object o) {
    if (!isNull(o).booleanValue()) {
      o.hashCode();
    }
  }

  int ternary(@Nullable Object o) {
    return isNN(o).booleanValue() ? o.hashCode() : 0;
  }

  boolean conditionalAnd(@Nullable Object o) {
    return isNN(o).booleanValue() && o.hashCode() > 0;
  }

  int intValue(Integer i) {
    return i.intValue() + i.intValue();
  }
}
