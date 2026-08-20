// The parameter of a lambda that is nested in another lambda takes its type from the type argument
// inference of the invocation that the outer lambda is an argument of.  Here that type is
// `@Nullable String`, so the dereference below is an error.
//
// The error is not issued if the parameter's type is instead computed before that inference has
// determined it, while the function type's parameter type is not yet a proper type: the type of
// the parameter is then javac's Java type plus default qualifiers, `@NonNull String`.

import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LambdaParamTypeNotCached {

  static <A, B> List<B> map(List<A> in, Function<A, B> f) {
    throw new AssertionError();
  }

  static <A, B> B apply(A a, Function<A, B> f) {
    throw new AssertionError();
  }

  static void requiresNonNull(@NonNull String s) {}

  void m(List<@Nullable String> l) {
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
}
