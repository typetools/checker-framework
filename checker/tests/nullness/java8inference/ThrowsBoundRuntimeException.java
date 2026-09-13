// JLS 18.4, resolution: when the bound set contains the bound `throws αi` and *every* proper upper
// bound of αi is a supertype of RuntimeException, then Ti = RuntimeException.  Otherwise Ti is the
// greatest lower bound of the proper upper bounds, as for any other variable.
//
// `Resolution.resolveWithUpperBounds` used to set its `useRuntimeException` flag as soon as *some*
// proper upper bound was a supertype of RuntimeException, so a variable whose bounds included both
// Exception and IOException was instantiated to RuntimeException.  That instantiation contradicts
// the bound `αi <: IOException`, so incorporation produced false and resolution fell back to the
// capture-based algorithm, which instantiates the variable to a fresh type variable instead of to
// the greatest lower bound.
//
// In `nullableBox` below, E has the proper upper bounds `@Nullable Exception` (its declared bound)
// and `@Nullable IOException` (from the `Box<? super E>` argument).  Only the latter rules out
// RuntimeException, so E is `@Nullable IOException` and `list.add(null)` is legal.  Before the fix,
// E became the fresh type variable `capture#01 extends @Nullable IOException`, whose lower bound is
// @NonNull, and the call was reported as an [argument] error.
//
// The lambda `list -> list.add(null)` is implicitly typed, so it is not pertinent to applicability
// and its body contributes no constraint on E; the body is checked against whatever E was inferred
// to be.  `list.add` is the only use of E in a contravariant position, which is what distinguishes
// `@Nullable IOException` from a type variable whose upper bound is `@Nullable IOException`.

import java.io.IOException;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ThrowsBoundRuntimeException {

  interface ThrowingRunnable<E extends @Nullable Exception> {
    void run() throws E;
  }

  interface Box<T extends @Nullable Object> {}

  interface Adder<E extends @Nullable Exception> {
    void add(List<E> list);
  }

  static <E extends @Nullable Exception> void call(
      ThrowingRunnable<E> r, Box<? super E> b, Adder<E> a) throws E {
    throw new RuntimeException();
  }

  // E is inferred as glb(@Nullable Exception, @Nullable IOException) = @Nullable IOException.
  void nullableBox(Box<@Nullable IOException> box) throws Exception {
    call(() -> {}, box, list -> list.add(null));
  }

  // The same call, but the argument makes E's second proper upper bound @NonNull IOException, so
  // the inferred type argument is @NonNull IOException and null is not a legal element.
  void nonNullBox(Box<@NonNull IOException> box) throws Exception {
    // :: error: [argument]
    call(() -> {}, box, list -> list.add(null));
  }

  static <E extends @Nullable Exception> void callNoBox(ThrowingRunnable<E> r, Adder<E> a)
      throws E {
    throw new RuntimeException();
  }

  // Here @Nullable Exception is E's only proper upper bound, and it is a supertype of
  // RuntimeException, so the JLS 18.4 rule does apply: E is inferred as @NonNull RuntimeException
  // rather than as @Nullable Exception, and null is not a legal element.
  void allUpperBoundsPermitRuntimeException() throws Exception {
    // :: error: [argument]
    callNoBox(() -> {}, list -> list.add(null));
  }
}
