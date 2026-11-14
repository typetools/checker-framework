import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

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

  // The values of `x` and `y` might be the same object, even though neither type is a subtype of
  // the other.
  static <T extends D, U extends D> void typeVariableReceivers(T x, U y) {
    if (x.f != null) {
      y.f = null;
      // :: error: [dereference.of.nullable]
      x.f.toString();
    }
  }

  static class Box<T> {
    @Nullable Object g;
  }

  // The values of `x` and `y` might be the same object, even though neither type is a subtype of
  // the other.
  static void wildcardReceivers(Box<? extends Number> x, Box<? super Integer> y) {
    if (x.g != null) {
      y.g = null;
      // :: error: [dereference.of.nullable]
      x.g.toString();
    }
  }

  static class E {
    E next;
    @Nullable Object g;

    E(E next) {
      this.next = next;
    }

    @Pure
    E getNext() {
      return next;
    }
  }

  @Pure
  static E getNextStatic(E e) {
    return e.next;
  }

  // A method can read any field, not only the fields of its receiver and arguments.
  static void methodCallReceiver(E x, E fresh) {
    if (x.getNext().g != null) {
      x.next = fresh;
      // :: error: [dereference.of.nullable]
      x.getNext().g.toString();
    }
  }

  static void methodCallArgument(E x, E fresh) {
    if (getNextStatic(x).g != null) {
      x.next = fresh;
      // :: error: [dereference.of.nullable]
      getNextStatic(x).g.toString();
    }
  }
}
