// The type of the parameter of a lambda that is nested in another lambda is requested while type
// argument inference for the outer invocation is running, at a time when that inference cannot yet
// supply it.  The type computed then is provisional -- here it is javac's Java type plus default
// qualifiers, `@NonNull String` rather than `@Nullable String`.  Neither the parameter's type nor
// the type of any use of the parameter may be cached, or the lambda body is type-checked against
// the provisional type and the errors below are not issued.

import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LambdaParamTypeNestedLambda {

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
