// Used to cause crash similar to the one reported in #579
// https://github.com/typetools/checker-framework/issues/579
// Issue 579 test case is in checker/tests/nullness/java8/Issue579.java
// A similar test case appears in checker/tests/nullness/InferTypeArgsConditionalExpression.java

public class InferTypeArgsConditionalExpression {

  public <T> void foo(Generic<T> real, Generic<? super T> other, boolean flag) {
    bar(flag ? real : other);
  }

  <Q> void bar(Generic<? extends Q> param) {}

  interface Generic<F> {}
}
