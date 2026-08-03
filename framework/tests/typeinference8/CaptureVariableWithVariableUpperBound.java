// Test input for TypeInference8FreshTypeVariableTest, which checks finding 1.16:
// `InferenceFactory.createFreshTypeVariable` must accept a null `upperBound`.
//
// The upper bound of `G`'s type parameter `T` is the type parameter `S`.  The return type of
// `make` is wildcard-parameterized and mentions the inference variable for `X`, so JLS 18.5.2.1
// creates the bound `G<a1, a2> = capture(G<?, ? extends X>)`.  The capture variable `a1` for `T`
// has no lower bound, and its only upper bound is a use of the capture variable `a2`, which
// `VariableBounds.upperBounds()` filters out.  `Resolution.resolveWithCapture` therefore passes a
// null upper bound (and a null lower bound) to `InferenceFactory.createFreshTypeVariable`.
//
// This file is not part of any per-directory test suite; the directory `framework/tests/
// typeinference8` is read only by TypeInference8FreshTypeVariableTest.

public class CaptureVariableWithVariableUpperBound {

  static class G<T extends S, S> {}

  static <X> G<?, ? extends X> make(X x) {
    throw new RuntimeException();
  }

  static void useWithParameterizedTarget(String s) {
    G<?, ?> g = make(s);
  }

  static void useWithObjectTarget(String s) {
    Object o = make(s);
  }
}
