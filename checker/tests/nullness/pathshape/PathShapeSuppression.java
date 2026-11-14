// Tests that the Path Shape Checker's warnings are suppressible with the prefix "pathshape".

import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PathShapeSuppression {

  @SuppressWarnings("pathshape:assignment")
  void suppressed(Path p) {
    @HasName Path q = p;
  }

  @SuppressWarnings("pathshape")
  void suppressedAll(Path p) {
    @HasName Path q = p;
  }

  // The "nullness" prefix does not suppress the Path Shape Checker's warnings.
  @SuppressWarnings("nullness:assignment")
  void notSuppressed(Path p, @Nullable Object o) {
    @NonNull Object n = o;
    // :: error: (assignment)
    @HasName Path q = p;
  }
}
