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

  @Pure
  static E first(E[] es) {
    return es[0];
  }

  // A method can read any array element, not only the elements of arrays in its receiver and
  // arguments.
  static void methodCallReadsArrayElement(E[] es, E fresh) {
    if (first(es).g != null) {
      es[0] = fresh;
      // :: error: [dereference.of.nullable]
      first(es).g.toString();
    }
  }

  static class S {
    static @Nullable Object sf;
  }

  static class S1 extends S {}

  static class S2 extends S {}

  // `S1.sf` and `S2.sf` are the same location, even though neither class is a subclass of the
  // other.
  static void staticFieldThroughSubclasses() {
    if (S1.sf != null) {
      S2.sf = null;
      // :: error: [dereference.of.nullable]
      S1.sf.toString();
    }
  }

  // The values of `a` and `b` might be the same array, whose elements implement both interfaces.
  static void interfaceArrays(@Nullable Runnable[] a, @Nullable Comparable<?>[] b) {
    if (a[0] != null) {
      b[0] = null;
      // :: error: [dereference.of.nullable]
      a[0].run();
    }
  }

  // The values of `a` and `b` cannot be the same array:  `String` is final and does not implement
  // `Runnable`.
  static void unrelatedArrays(@Nullable Runnable[] a, @Nullable String[] b) {
    if (a[0] != null) {
      b[0] = null;
      a[0].run();
    }
  }
}
