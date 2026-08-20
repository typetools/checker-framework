// Type argument inference computes the bound set B2 of an invocation in the body of an implicitly
// typed lambda.  Doing so computes the type of each argument of that invocation, at a time when the
// lambda's parameters do not yet have types.  If those types were cached, the type-check of the
// lambda body would use them and would not issue the errors below.

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LambdaBodyNestedInvocation {

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
