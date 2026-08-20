// Test that an implicitly typed lambda parameter's type is taken from the running type argument
// inference, rather than from javac's Java type plus default qualifiers.
//
// Additional test case in checker/tests/tainting/LambdaParamTypeFromInference.java

import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LambdaParamTypeFromInference {

  static <T, R> R map(T t, Function<T, R> f) {
    return f.apply(t);
  }

  // The type of `p` is requested while inference for `map` is still running, so it cannot be
  // computed through the usual target-type machinery.  Falling back to javac's Java type plus
  // default qualifiers is unsound here, not merely imprecise: it would strengthen `p` from
  // `@Nullable String` to `@NonNull String`, and the dereference below would go unreported.
  //
  // The error is issued while checking the lambda body, after inference for `map` has finished.
  // The type of `p` is recomputed then, by re-running that inference, which again asks the running
  // inference for the type of `p`.
  void deref(@Nullable String s) {
    // :: error: [dereference.of.nullable]
    map(s, p -> p.hashCode());
  }
}
