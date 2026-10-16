// `InferenceFactory.createFreshTypeVariable` must accept a null `upperBound`, and the fresh type
// variable it creates must have an annotation on each of its bounds.
//
// The upper bound of `G`'s type parameter `T` is the type parameter `S`.  The return type of
// `make` is wildcard-parameterized and mentions the inference variable for `X`, so JLS 18.5.2.1
// creates the bound `G<a1, a2> = capture(G<?, ? extends X>)`.  The capture variable `a1` for `T`
// has no lower bound, and its only upper bound is a use of the capture variable `a2`, which
// `VariableBounds.upperBounds()` filters out.  `Resolution.resolveWithCapture` therefore passes a
// null upper bound (and a null lower bound) to `InferenceFactory.createFreshTypeVariable`.

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
