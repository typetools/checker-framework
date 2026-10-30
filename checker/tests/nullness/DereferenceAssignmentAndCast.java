import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Dereferencing an assignment or a cast refines the expressions that have the same value.
public class DereferenceAssignmentAndCast {

  String f;

  DereferenceAssignmentAndCast(String f) {
    this.f = f;
  }

  @NonNull String methodAccessOfAssignment(@Nullable String s, @Nullable String t) {
    // :: error: [dereference.of.nullable]
    (s = t).length();
    s.toString();
    return t;
  }

  @NonNull Object methodAccessOfCast(@Nullable Object o) {
    // :: error: [dereference.of.nullable]
    ((String) o).length();
    return o;
  }

  @NonNull Object fieldAccessOfAssignmentOfCast(
      @Nullable DereferenceAssignmentAndCast d, @Nullable Object o) {
    // :: error: [dereference.of.nullable]
    (d = (DereferenceAssignmentAndCast) o).f.toString();
    d.toString();
    return o;
  }

  @NonNull Object arrayAccessOfCast(@Nullable Object o) {
    // :: error: [accessing.nullable]
    ((String[]) o)[0].toString();
    return o;
  }

  void throwOfAssignment(@Nullable RuntimeException e, @Nullable RuntimeException f) {
    try {
      // :: error: [throwing.nullable]
      throw e = f;
    } finally {
      // e and f may be null if the throw itself threw NullPointerException.
      // :: error: [dereference.of.nullable]
      e.toString();
    }
  }
}
