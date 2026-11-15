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

  static class Holder {
    @Nullable Holder next;
    int count;

    @org.checkerframework.dataflow.qual.Pure
    int index() {
      return next == null ? 0 : 1;
    }

    @org.checkerframework.dataflow.qual.Pure
    int countIndex() {
      return count;
    }
  }

  // Assigning `h.next` might change `h.index()`, and therefore `a[h.index()]`, because the method's
  // result might depend on `h.next`.
  static void arrayIndexedByMethodCall(@Nullable String[] a, Holder h) {
    if (a[h.index()] != null) {
      h.next = null;
      // :: error: [dereference.of.nullable]
      a[h.index()].length();
    }
  }

  // Assigning `h.count` might change `h.countIndex()`, even though the type of `h.count` is
  // unrelated to the type of `h`.
  static void arrayIndexedByMethodCallPrimitiveField(@Nullable String[] a, Holder h) {
    if (a[h.countIndex()] != null) {
      h.count = 1;
      // :: error: [dereference.of.nullable]
      a[h.countIndex()].length();
    }
  }
}
