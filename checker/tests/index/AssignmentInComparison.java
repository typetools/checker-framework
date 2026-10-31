import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.dataflow.qual.Pure;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  @NonNegative int f;

  @Pure
  @NonNegative int size() {
    return f;
  }

  static void localTarget(@NonNegative int i, @NonNegative int j) {
    if ((i = j) > 0) {
      @Positive int x = i;
      @Positive int y = j;
    }
  }

  static void localTargetMethodCallRhs(@NonNegative int i, AssignmentInComparison n) {
    if ((i = n.size()) > 0) {
      @Positive int x = i;
      @Positive int y = n.size();
    }
  }

  static void fieldTargetMethodCallRhs(AssignmentInComparison n) {
    if ((n.f = n.size()) > 0) {
      @Positive int x = n.f;
      // The assignment to `n.f` might change the result of `n.size()`.
      // :: error: [assignment]
      @Positive int y = n.size();
    }
  }
}
