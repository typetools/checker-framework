// A try-with-resources statement calls close() only if the resource is non-null.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class TryWithResourcesNullResource {
  static @Nullable Object sf;

  static class Res implements AutoCloseable {
    @Override
    @EnsuresNonNull("TryWithResourcesNullResource.sf")
    public void close() {
      sf = "";
    }
  }

  void existingVariable(@Nullable Res r) {
    try (r) {}
    // :: error: [dereference.of.nullable]
    r.toString();
  }

  void existingNonNullVariable(Res r) {
    try (r) {}
    r.toString();
  }

  void postconditionNullableResource(@Nullable Res r) {
    sf = null;
    try (Res r2 = r) {}
    // :: error: [dereference.of.nullable]
    sf.toString();
  }

  void postconditionNewResource() {
    sf = null;
    try (Res r2 = new Res()) {}
    sf.toString();
  }

  // The CFG contains a path on which the resource is null and close() is not called.  The Nullness
  // Checker does not determine that the path is infeasible.
  void postconditionNonNullResource(Res r) {
    sf = null;
    try (Res r2 = r) {}
    // :: error: [dereference.of.nullable]
    sf.toString();
  }
}
