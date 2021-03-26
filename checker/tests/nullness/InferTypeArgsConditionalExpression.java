// Used to cause crash similar to the one reported in #579
// https://github.com/typetools/checker-framework/issues/579
// Issue 579 test case is in checker/tests/nullness/java8/Issue579.java
// A similar test case appears in
// checker-framework/framework/tests/all-systems/InferTypeArgsConditionalExpression.java

import org.checkerframework.checker.nullness.qual.NonNull;

public class InferTypeArgsConditionalExpression {

  public <T> void foo(Generic<T> real, Generic<? super T> other, boolean flag) {
    // :: error: (type.argument.type.incompatible)
    bar(flag ? real : other);
  }

  <@NonNull Q extends @NonNull Object> void bar(Generic<? extends Q> parm) {}

  interface Generic<F> {}
}
