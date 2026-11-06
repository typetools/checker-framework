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
}
