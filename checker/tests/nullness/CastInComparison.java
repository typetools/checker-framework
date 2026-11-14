import org.checkerframework.checker.nullness.qual.Nullable;

// Comparing a cast refines the cast's operand, if the cast does not change the value.
public class CastInComparison {

  void cast(@Nullable Object o) {
    if ((String) o != null) {
      o.toString();
    }
  }

  void castOfAssignment(@Nullable Object s, @Nullable Object t) {
    if ((String) (s = t) != null) {
      s.toString();
      t.toString();
    }
  }

  void assignmentOfCast(@Nullable String s, @Nullable Object t) {
    if ((s = (String) t) != null) {
      s.toString();
      t.toString();
    }
  }

  static Object nonNull(@Nullable Object o) {
    return "";
  }

  // The right operand assigns `x` after the left operand reads it, so the comparison says nothing
  // about the new value of `x`.
  void assignmentInRightOperand(@Nullable Object x) {
    if (((String) x) == nonNull(x = null)) {
      // :: error: (dereference.of.nullable)
      x.toString();
    }
  }

  // The left operand assigns `x` before the right operand reads it, so refining `x` is sound.
  void assignmentInLeftOperand(@Nullable Object x) {
    if (nonNull(x = null) == ((String) x)) {
      x.toString();
    }
  }
}
