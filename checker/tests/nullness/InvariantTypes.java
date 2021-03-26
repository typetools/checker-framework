import org.checkerframework.checker.nullness.qual.*;

public class InvariantTypes {
  // The RHS is @NonNull [], but context decides to make it @Nullable
  @Nullable Object[] noa = {"non-null!"};

  // Type for array creation is propagated from LHS
  @MonotonicNonNull Object[] f = new Object[5];

  void testAsLocal() {
    @MonotonicNonNull Object[] lo;
    lo = new Object[5];
    // :: error: (assignment.type.incompatible)
    lo[0] = null;
    lo[0] = new Object();
    // :: error: (dereference.of.nullable)
    lo[1].toString();
  }

  // Type for array creation is propagated from LHS
  @SuppressWarnings("invalid.polymorphic.qualifier.use")
  @PolyNull Object[] po = new Object[5];

  void testDecl(@MonotonicNonNull Object[] p) {}

  void testCall() {
    // Type for array creation is propaged from parameter type
    testDecl(new Object[5]);
  }
}
