// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// `id(true)` is in the guard of a case of a switch expression, so its target type is boolean, not
// the type of a formal parameter of `use`.  TreePathUtil.getContextForPolyExpression must not walk
// up from the guard through the CaseTree and the SwitchExpressionTree to the invocation `use(...)`.

public class SwitchExpressionGuard {

  static <T> T id(T p) {
    return p;
  }

  static void use(Object o) {}

  static void m(Object x) {
    use(
        switch (x) {
          case String s when id(true) -> 1;
          default -> 2;
        });
  }
}
