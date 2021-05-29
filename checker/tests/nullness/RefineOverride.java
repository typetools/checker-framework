// Test case for issue #611: https://github.com/typetools/checker-framework/issues/611
// @skip-test Disabled until the issue is fixed.

import org.checkerframework.checker.nullness.qual.*;

public class RefineOverride {

  void m(Sub<@Nullable String> snb, Sub<@NonNull String> snn) {
    snb.m7(null);
    snn.m7(null);
  }

  class Super<T> {

    void m1(@NonNull String s) {}

    void m2(@NonNull String s) {}

    void m5(@NonNull String s) {}

    void m6(@Nullable String s) {}

    void m7(T s) {}

    void m11(@NonNull String s1, @NonNull String s2) {}

    void m12(@NonNull String s1, @Nullable String s2) {}

    void m13(@Nullable String s1, @NonNull String s2) {}

    void m14(@Nullable String s1, @Nullable String s2) {}

    void m15(T s1, T s2) {}

    void m16(@NonNull T s1, @NonNull T s2) {}

    void m17(@NonNull T s1, @Nullable T s2) {}

    void m18(@Nullable T s1, @NonNull T s2) {}

    void m19(@Nullable T s1, @Nullable T s2) {}

    void m21(@Nullable String[] a) {}

    void m22(@NonNull String[] a) {}

    void m23(@Nullable String[] a) {}

    void m24(@NonNull String[] a) {}

    void m25(T[] a) {}

    void m26(@Nullable T[] a) {}

    void m27(@NonNull T[] a) {}

    void m28(@Nullable T[] a) {}

    void m29(@NonNull T[] a) {}
  }

  class Sub<T> extends Super<T> {

    @Override
    void m1(@Nullable String s) {}

    @Override
    void m2(@PolyNull String s) {}

    // In the following declarations, all previously-valid invocations remain
    // valid, so the compiler should permit the overriding.

    // Case 1.  A single parameter type is changed from anything to @PolyNull
    // in an overriding method.

    @Override
    void m5(@PolyNull String s) {}

    // TODO: should be legal
    @Override
    void m6(@PolyNull String s) {}

    // TODO: should be legal
    @Override
    void m7(@PolyNull T s) {}

    // Case 2.  Multiple parameter types are changed to @PolyNull in an
    // overriding method.

    // (The types for m14 might be better written as "@PolyNull(1)
    // ... @PolyNull(2)", but all invocations remain valid.

    @Override
    void m11(@PolyNull String s1, @PolyNull String s2) {}

    // TODO: should be legal
    @Override
    void m12(@PolyNull String s1, @PolyNull String s2) {}

    // TODO: should be legal
    @Override
    void m13(@PolyNull String s1, @PolyNull String s2) {}

    // TODO: should be legal
    @Override
    void m14(@PolyNull String s1, @PolyNull String s2) {}

    // TODO: should be legal
    @Override
    void m15(@PolyNull T s1, @PolyNull T s2) {}

    @Override
    void m16(@PolyNull T s1, @PolyNull T s2) {}

    // TODO: should be legal
    @Override
    void m17(@PolyNull T s1, @PolyNull T s2) {}

    // TODO: should be legal
    @Override
    void m18(@PolyNull T s1, @PolyNull T s2) {}

    // TODO: should be legal
    @Override
    void m19(@PolyNull T s1, @PolyNull T s2) {}

    // Case 3.  Expand the element type of an array.
    // The new permissible types are not supertypes of the old types,
    // but they still expand the set of permitted invocations.

    // :: error: (override.param)
    @Override
    void m21(@NonNull String[] a) {}

    // :: error: Changing incompatibly to forbid old invocations is not permitted.
    @Override
    void m22(@Nullable String[] a) {}

    // TODO: should be legal
    @Override
    void m23(@PolyNull String[] a) {}

    @Override
    void m24(@PolyNull String[] a) {}

    @Override
    void m25(@PolyNull T[] a) {}

    // :: error: (override.param)
    @Override
    void m26(@NonNull T[] a) {}

    // :: error: Changing incompatibly to forbid old invocations is not permitted.
    @Override
    void m27(@Nullable T[] a) {}

    // TODO: should be legal
    @Override
    void m28(@PolyNull T[] a) {}

    @Override
    void m29(@PolyNull T[] a) {}
  }

  class Super2<T> {

    void t1(String s) {}

    void t2(String s) {}

    void t3(@Nullable String s) {}

    void t4(String s) {}

    void t5(String[] s) {}

    void t6(T s) {}

    void t7(T s) {}

    void t8(T[] s) {}

    void t9(T[] s) {}
  }

  class Sub2<T> extends Super2<T> {

    @Override
    void t1(String s) {}

    @Override
    void t2(@Nullable String s) {}

    // :: error: (override.param)
    @Override
    void t3(String s) {}

    @Override
    void t4(@PolyNull String s) {}

    @Override
    void t5(@PolyNull String[] s) {}

    @Override
    void t6(@Nullable T s) {}

    // TODO: should be legal
    @Override
    void t7(@PolyNull T s) {}

    @Override
    void t8(@Nullable T[] s) {}

    // TODO: should be legal
    @Override
    void t9(@PolyNull T[] s) {}
  }
}
