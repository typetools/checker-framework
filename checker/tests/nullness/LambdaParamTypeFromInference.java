// Tests that an implicitly typed lambda parameter's type is taken from the running type argument
// inference, rather than from javac's Java type plus default qualifiers.
//
// Additional test case in checker/tests/tainting/LambdaParamTypeFromInference.java

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LambdaParamTypeFromInference {

  static class FromInference {

    static <T, R> R map(T t, Function<T, R> f) {
      return f.apply(t);
    }

    // The type of `p` is requested while inference for `map` is still running, so it cannot be
    // computed through the usual target-type machinery.  Falling back to javac's Java type plus
    // default qualifiers is unsound here, not merely imprecise: it would strengthen `p` from
    // `@Nullable String` to `@NonNull String`, and the dereference below would go unreported.
    //
    // The error is issued while checking the lambda body, after inference for `map` has finished.
    // The type of `p` is recomputed then, by re-running that inference, which again asks the
    // running inference for the type of `p`.
    void deref(@Nullable String s) {
      // :: error: [dereference.of.nullable]
      map(s, p -> p.hashCode());
    }
  }

  // Type argument inference computes the bound set B2 of an invocation in the body of an implicitly
  // typed lambda.  Doing so computes the type of each argument of that invocation, at a time when
  // the lambda's parameters do not yet have types.  If those types were cached, the type-check of
  // the lambda body would use them and would not issue the errors below.
  static class BodyNestedInvocation {

    static <A, B> List<B> map(List<A> in, Function<? super A, ? extends B> f) {
      throw new UnsupportedOperationException();
    }

    static <X> X nonNullOnly(@NonNull X x) {
      return x;
    }

    // The lambda parameter is the receiver of a call in an argument of the nested invocation.
    void dereference(List<@Nullable String> l) {
      // :: error: (dereference.of.nullable)
      List<Integer> lengths = map(l, s -> nonNullOnly(s.length()));
    }

    // The lambda parameter is itself an argument of the nested invocation.
    void argument(List<@Nullable String> l) {
      // :: error: (argument)
      List<String> strings = map(l, s -> nonNullOnly(s));
    }

    // The same, for a method of the JDK.
    void jdkArgument(List<@Nullable String> l) {
      // :: error: (argument)
      List<String> strings = map(l, s -> Objects.requireNonNull(s));
    }
  }

  // The type of the parameter of a lambda that is nested in another lambda is requested while type
  // argument inference for the outer invocation is running, at a time when that inference cannot
  // yet supply it.  The type computed then is provisional -- here it is javac's Java type plus
  // default qualifiers, `@NonNull String` rather than `@Nullable String`.  Neither the parameter's
  // type nor the type of any use of the parameter may be cached, or the lambda body is type-checked
  // against the provisional type and the errors below are not issued.
  static class NestedLambda {

    static <A, B> List<B> map(List<A> in, Function<A, B> f) {
      throw new AssertionError();
    }

    static <A, B> B apply(A a, Function<A, B> f) {
      throw new AssertionError();
    }

    static void requiresNonNull(@NonNull String s) {}

    void dereference(List<@Nullable String> l) {
      // :: error: (dereference.of.nullable)
      List<Integer> r = map(l, s -> apply(s, t -> t.hashCode()));
    }

    // The same, with a block-bodied inner lambda.
    void dereferenceBlockBody(List<@Nullable String> l) {
      List<Integer> r =
          map(
              l,
              s ->
                  apply(
                      s,
                      t -> {
                        // :: error: (dereference.of.nullable)
                        return t.hashCode();
                      }));
    }

    void argument(List<@Nullable String> l) {
      List<Integer> r =
          map(
              l,
              s ->
                  apply(
                      s,
                      t -> {
                        // :: error: (argument)
                        requiresNonNull(t);
                        return t.hashCode();
                      }));
    }
  }
}
