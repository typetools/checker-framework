// Test input for TypeInference8ArgumentIndexTest.
//
// `id(true)` is a poly expression, so type argument inference computes a target type for
// `id(true)`.  TreePathUtil.getContextForPolyExpression walks up from `id(true)` through the
// CaseTree and the SwitchExpressionTree to the invocation `use(...)`, and returns that invocation.
// But `id(true)` is in the *guard* of the case, not in a result expression of the switch
// expression, so InferenceFactory.isArgument does not find `id(true)` in any argument of
// `use(...)`.
//
// Type argument inference therefore fails on this file.  TypeInference8ArgumentIndexTest checks
// only the diagnostic that reports the failure, not that this file typechecks.
//
// The `when` clause requires Java 21; TypeInference8ArgumentIndexTest is skipped on earlier JDKs.

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
