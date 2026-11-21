import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

// The boxing conversion of a returned boolean keeps the facts that hold when the boolean is true or
// false, so a method that returns Boolean can satisfy a conditional postcondition.
public class BoxingKeepsConditionalRefinement {
  @Nullable Object f;

  @EnsuresNonNullIf(expression = "f", result = true)
  Boolean isNN() {
    return f != null;
  }

  @EnsuresNonNullIf(expression = "f", result = false)
  Boolean isNull() {
    return f == null;
  }

  // Inside a try block, the boxing call can end its block, and the return statement begins the
  // successor block.
  @EnsuresNonNullIf(expression = "f", result = true)
  Boolean isNNInTry() {
    try {
      return f != null;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @EnsuresNonNullIf(expression = "f", result = true)
  Boolean wrongResult() {
    // :: error: [contracts.conditional.postcondition]
    return f == null;
  }

  void use() {
    if (isNN()) {
      f.hashCode();
    }
    if (!isNull()) {
      f.hashCode();
    }
  }
}
