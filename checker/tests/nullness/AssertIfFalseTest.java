import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

public class AssertIfFalseTest {

  @org.checkerframework.dataflow.qual.Pure
  @Nullable Object get() {
    return "m";
  }

  @EnsuresNonNullIf(result = false, expression = "get()")
  boolean isGettable() {
    // don't bother with the implementation
    // :: error: (contracts.conditional.postcondition.not.satisfied)
    return false;
  }

  void simple() {
    // :: error: (dereference.of.nullable)
    get().toString();
  }

  void checkWrongly() {
    if (isGettable()) {
      // :: error: (dereference.of.nullable)
      get().toString();
    }
  }

  void checkCorrectly() {
    if (!isGettable()) {
      get().toString();
    }
  }

  /** Returns whether or not constant_value is a legal constant. */
  @EnsuresNonNullIf(result = false, expression = "#1")
  static boolean legalConstant(final @Nullable Object constant_value) {
    if ((constant_value == null)
        || ((constant_value instanceof Long) || (constant_value instanceof Double))) return true;
    return false;
  }

  void useLegalConstant1(@Nullable Object static_constant_value) {
    if (!legalConstant(static_constant_value)) {
      throw new AssertionError("unexpected constant class " + static_constant_value.getClass());
    }
  }

  void useLegalConstant2(@Nullable Object static_constant_value) {
    assert legalConstant(static_constant_value)
        : "unexpected constant class " + static_constant_value.getClass();
  }
}
