// Test that an implicitly typed lambda parameter's type is taken from the running type argument
// inference, rather than from javac's Java type plus default qualifiers.
//
// Additional test case in checker/tests/nullness/LambdaParamTypeFromInference.java

import java.util.function.Function;
import org.checkerframework.checker.tainting.qual.Untainted;

public class LambdaParamTypeFromInference {

  static <T, R> R map(T t, Function<T, R> f) {
    return f.apply(t);
  }

  static <T> T same(T t, Function<T, T> f) {
    return f.apply(t);
  }

  // The type of `p` is requested while inference for `map` is still running, so it cannot be
  // computed through the usual target-type machinery.  Falling back to javac's Java type plus
  // default qualifiers would give `p` the type `@Tainted String`, even though the lambda's target
  // type is `Function<@Untainted String, R>`, and that `@Tainted` would flow into R, causing both
  // an `assignment` error and a `type.arguments.not.inferred` error here.
  void m(@Untainted String s) {
    @Untainted String r = map(s, p -> p);
  }

  // Two type variables are essential above.  With `Function<T, T>`, T is still uninstantiated when
  // the lambda body's constraint is processed, so the running inference cannot supply the parameter
  // type and the fallback applies.  JLS 18.5.2.2 makes the same distinction: the input variables of
  // an implicitly typed lambda's constraint are the inference variables mentioned by the function
  // type's parameter types, not those in its return type.
  void parameterTypeNotYetKnown(@Untainted String s) {
    @Untainted String r = same(s, p -> p);
  }
}
