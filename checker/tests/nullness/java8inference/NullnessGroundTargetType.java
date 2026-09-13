// Tests JLS 18.5.3, Functional Interface Parameterization Inference.
//
// When an explicitly typed lambda targets a wildcard-parameterized functional interface type
// F<A1, ..., Am>, the ground target type is F<A'1, ..., A'm>, where each A'i is inferred from the
// lambda's declared parameter types; JLS 18.2.1 then requires <F<A'1, ..., A'm> <: F<A1, ..., Am>>.
// When the ground target type is instead computed as the non-wildcard parameterization (JLS 9.9) of
// F<A1, ..., Am>, that subtyping constraint is trivially satisfied, and a lambda parameter whose
// qualifier is inconsistent with the wildcard's bound goes undetected.
//
// javac rejects the erasure-level analogue of each error below; for example, for
//   static <T> T ext(Fn<? extends String, ? extends T> f)
// the call ext((Object s) -> new Object()) is rejected with
//   "Fn<Object,Object> cannot be converted to Fn<? extends String,? extends Object>".

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NullnessGroundTargetType {

  interface Fn<A, B> {
    B apply(A a);
  }

  interface Fn2<A, C, B> {
    B apply(A a, C c);
  }

  static <T> T ext(Fn<? extends @NonNull String, ? extends T> f) {
    throw new RuntimeException();
  }

  static <T> T ext2(Fn2<? extends @NonNull String, ? extends @NonNull String, ? extends T> f) {
    throw new RuntimeException();
  }

  void oneParameter() {
    // The ground target type is Fn<@Nullable String, ...>, which is not a subtype of
    // Fn<? extends @NonNull String, ...>.
    // :: error: (type.arguments.not.inferred)
    Object bad = ext((@Nullable String s) -> new Object());
    // The ground target type is Fn<@NonNull String, ...>.
    Object good = ext((@NonNull String s) -> new Object());
  }

  void twoParameters() {
    // Only the second lambda parameter is inconsistent with the target type.
    // :: error: (type.arguments.not.inferred)
    Object bad = ext2((@NonNull String s, @Nullable String t) -> new Object());
    Object good = ext2((@NonNull String s, @NonNull String t) -> new Object());
  }

  void implicitlyTypedLambda() {
    // An implicitly typed lambda does not use 18.5.3; its ground target type is the non-wildcard
    // parameterization (9.9) of the target type.
    Object good = ext(s -> new Object());
  }
}
