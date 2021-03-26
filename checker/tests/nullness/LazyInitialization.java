import org.checkerframework.checker.nullness.qual.*;

public class LazyInitialization {
  @Nullable Object nullable;
  @NonNull Object nonnull;
  @MonotonicNonNull Object lazy;
  @MonotonicNonNull Object lazy2 = null;
  final @Nullable Object lazy3;

  public LazyInitialization(@Nullable Object arg) {
    lazy3 = arg;
    nonnull = new Object();
  }

  void randomMethod() {}

  void testAssignment() {
    lazy = "m";
    // :: error: (assignment.type.incompatible)
    lazy = null; // null
  }

  void testLazyBeingNull() {
    // :: error: (dereference.of.nullable)
    nullable.toString(); // error
    nonnull.toString();
    // :: error: (dereference.of.nullable)
    lazy.toString(); // error
    // :: error: (dereference.of.nullable)
    lazy3.toString(); // error
  }

  void testAfterInvocation() {
    nullable = "m";
    nonnull = "m";
    lazy = "m";
    if (lazy3 == null) {
      return;
    }

    randomMethod();

    // :: error: (dereference.of.nullable)
    nullable.toString(); // error
    nonnull.toString();
    lazy.toString();
    lazy3.toString();
  }

  private double @MonotonicNonNull [] intersect;

  public void check_modified(double[] a, int count) {
    if (intersect != null) {
      double @NonNull [] nnda = intersect;
    }
  }

  class PptRelation1 {
    public void init_hierarchy_new(PptTopLevel ppt, Object eq) {
      ppt.equality_view = eq;
      ppt.equality_view.toString();
    }
  }

  class PptTopLevel {
    public @MonotonicNonNull Object equality_view;
  }

  class PptRelation1b {
    // This is the same code as in PptRelation1, but comes after the class
    // declaration of PptTopLevel. This works as expected.
    public void init_hierarchy_new(PptTopLevel ppt, Object eq) {
      ppt.equality_view = eq;
      ppt.equality_view.toString();
    }
  }

  class PptRelation2 {
    public @MonotonicNonNull Object equality_view2;

    public void init_hierarchy_new(PptRelation2 pr1, PptRelation2 pr2, Object eq) {
      // :: error: (dereference.of.nullable)
      pr1.equality_view2.toString();

      pr1.equality_view2 = eq;
      pr1.equality_view2.toString();

      // :: error: (dereference.of.nullable)
      pr2.equality_view2.toString();
      // :: error: (dereference.of.nullable)
      this.equality_view2.toString();

      pr2.equality_view2 = eq;
      pr2.equality_view2.toString();

      this.equality_view2 = eq;
      this.equality_view2.toString();
    }
  }
}
