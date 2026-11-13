import org.checkerframework.checker.nullness.qual.Nullable;

// An assignment discards the refinement of an expression only if the expression reads the
// location that is assigned.
public class AssignmentChangesLocations {

  static class C {
    @Nullable Object f;
    Object o = "";
  }

  // Assigning `x.o` changes neither the local variable `x` nor the field `x.f`, even though the
  // value of `x` can be the same object as the value of `x.o`.
  static void otherFieldOfObjectType(C x) {
    if (x.f != null) {
      x.o = "";
      x.f.toString();
    }
  }

  // Assigning `a[i]` changes `a[i][0]`, because `a[i][0]` reads `a[i]`.
  static void elementOfReassignedArray(@Nullable String[][] a, int i) {
    if (a[i][0] != null) {
      a[i] = new @Nullable String[1];
      // :: error: [dereference.of.nullable]
      a[i][0].length();
    }
  }

  static class D {
    @Nullable D f;
    @Nullable Object g;
  }

  // Assigning `y.f` changes `x.f.g` if `x` and `y` are the same object, because `x.f.g` reads
  // `x.f`.
  static void fieldOfReassignedField(D x, D y) {
    if (x.f != null && x.f.g != null) {
      y.f = new D();
      // :: error: [dereference.of.nullable]
      x.f.g.toString();
    }
  }
}
