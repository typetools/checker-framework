// A type variable may be mentioned only in the enclosing type of an inner class type, as in
// `Outer<T>.Inner`.  Such a type is not a proper type, so inference must treat it as one that
// mentions an inference variable, and reduction must derive a bound from the enclosing type.

// Two limitations elsewhere in the Checker Framework shape how this file is written:
//  * `DefaultTypeHierarchy` ignores enclosing types, so an assignment whose only mismatch is in an
//    enclosing type -- such as `Outer<@NonNull String>.Inner a = nullableInner();` -- is accepted.
//    Therefore, to observe an inferred qualifier, the methods below return `T` itself.
//  * A type written in source as `Outer<@Nullable String>.Inner` loses the `@Nullable` unless the
//    type is read back from an element, so the tests below obtain such a value from a method's
//    return type rather than from a local variable or a formal parameter.

import java.util.List;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EnclosingType {

  static class Outer<T> {
    class Inner {}

    class Inner2<S> {}
  }

  static <T> Outer<T>.Inner make(T t) {
    throw new RuntimeException();
  }

  static <T> Outer<T>.Inner id(Outer<T>.Inner p) {
    return p;
  }

  static <T> T fromInner(Outer<T>.Inner p) {
    throw new RuntimeException();
  }

  static <T> T fromInnerList(List<Outer<T>.Inner> p) {
    throw new RuntimeException();
  }

  static <T> T fromInnerArray(Outer<T>.Inner[] p) {
    throw new RuntimeException();
  }

  static <T> T fromSupplier(Supplier<Outer<T>.Inner> p) {
    throw new RuntimeException();
  }

  static <T, S> T fromInner2(Outer<T>.Inner2<S> p) {
    throw new RuntimeException();
  }

  static <T extends Comparable<T>> T fromComparableInner(Outer<T>.Inner p) {
    throw new RuntimeException();
  }

  static <T> void twoUses(Outer<T>.Inner p, T t) {}

  // These methods supply values whose enclosing type argument carries a non-default qualifier.

  static Outer<@Nullable String>.Inner nullableInner() {
    throw new RuntimeException();
  }

  static List<Outer<@Nullable String>.Inner> nullableInnerList() {
    throw new RuntimeException();
  }

  static Outer<@Nullable String>.Inner[] nullableInnerArray() {
    throw new RuntimeException();
  }

  static Supplier<Outer<@Nullable String>.Inner> nullableInnerSupplier() {
    throw new RuntimeException();
  }

  static Outer<@Nullable String>.Inner2<@NonNull String> nullableInner2() {
    throw new RuntimeException();
  }

  static Outer<@NonNull String>.Inner nonNullInner() {
    throw new RuntimeException();
  }

  // Every call in the next two methods crashed with `type.argument.inference.crashed` before the
  // enclosing type was scanned for inference variables.

  void useMake(@Nullable String ns, @NonNull String nn) {
    // T is inferred as `@Nullable String` from the argument and as `@NonNull String` from the
    // target type's enclosing type; those are contradictory.
    // :: error: (type.arguments.not.inferred)
    Outer<@NonNull String>.Inner a = make(ns);
    // T is inferred as `@NonNull String` from the argument, which satisfies the target type's
    // `@Nullable String` bound.
    Outer<@Nullable String>.Inner b = make(nn);
  }

  void useOther(Outer<@Nullable String>.Inner inner, @NonNull String nn) {
    Outer<@NonNull String>.Inner c = id(inner);
    Object o = fromInner(inner);
    twoUses(inner, nn);
  }

  // In each method below, the enclosing type is the type variable's only occurrence in the formal
  // parameter, so the call checks that reduction derives a bound from the enclosing type.

  void bareInner() {
    @Nullable String ok = fromInner(nullableInner());
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    @NonNull String bad = fromInner(nullableInner());
    @NonNull String alsoOk = fromInner(nonNullInner());
  }

  void nestedInGeneric() {
    @Nullable String ok = fromInnerList(nullableInnerList());
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    @NonNull String bad = fromInnerList(nullableInnerList());
  }

  void nestedInArray() {
    @Nullable String ok = fromInnerArray(nullableInnerArray());
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    @NonNull String bad = fromInnerArray(nullableInnerArray());
  }

  void nestedInFunctionalInterface() {
    @Nullable String ok = fromSupplier(nullableInnerSupplier());
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    @NonNull String bad = fromSupplier(nullableInnerSupplier());
  }

  void innerWithOwnTypeArgument() {
    @Nullable String ok = fromInner2(nullableInner2());
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    @NonNull String bad = fromInner2(nullableInner2());
  }

  void recursiveBound() {
    @NonNull String ok = fromComparableInner(nonNullInner());
    // T is inferred as `@Nullable String`, which does not satisfy the bound
    // `Comparable<@Nullable String>`.
    // :: error: (type.arguments.not.inferred)
    @Nullable String bad = fromComparableInner(nullableInner());
  }
}
