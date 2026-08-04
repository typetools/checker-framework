// This file contains no test code of its own.  The actual tests are performed by
// DependenciesTests, which builds inference variables out of the declarations below and then
// checks the behavior of
// org.checkerframework.framework.util.typeinference8.types.Dependencies.
//
// The declarations are here, rather than being synthesized by the test code, because creating an
// inference variable requires a real declaration to read an AnnotatedTypeMirror from.
public class Typeinference8Dependencies {

  /**
   * Used to create a proper type whose three type arguments can be replaced by inference variables.
   */
  Holder<String, String, String> holder;

  /** Used only as the key under which the {@code Theta} of {@link #holder} is cached. */
  Runnable thetaKeyLambda = () -> {};

  /** Declares the three type variables that become inference variables. */
  static class Holder<Z1, Z2, Z3> {}
}
