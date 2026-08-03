// Test that incorporation (JLS 18.3) runs to a fixed point.
//
// `BoundSet.incorporateToFixedPoint` used to *assign* rather than or-assign to the
// "did anything change?" flag on each iteration over the inference variables, so only the last
// variable determined whether another incorporation pass was made.  When the last variable is
// quiet in a pass but an earlier one is not, incorporation stopped early and bounds implied by
// 18.3.1 were never derived.
//
// In `withTrailingUnusedTypeParameter` below, the bound `U <: List<@Odd String>` (from the
// argument, via the `? super U` wildcard) implies `T = List<@Odd String>`, which in turn implies
// `S = @Odd String`.  Deriving the second of those requires a further incorporation pass, but W --
// the last inference variable -- has nothing to do, so incorporation used to stop after the first
// pass.  `S` was then constrained only by `Object`, and inference reported a spurious
// `type.arguments.not.inferred` error.

import java.util.List;
import org.checkerframework.framework.testchecker.util.Odd;

public class TypeInferenceIncorporationFixedPoint {

  static class Box<X> {}

  // W is not used; it is what makes the last inference variable quiet.
  static <S, T extends List<S>, U extends List<T>, W> void withTrailingUnusedTypeParameter(
      Box<? super U> b) {}

  static void useWithTrailingUnusedTypeParameter(Box<List<List<@Odd String>>> b) {
    withTrailingUnusedTypeParameter(b);
  }

  // The same method without the trailing type parameter, which needs one fewer incorporation pass.
  static <S, T extends List<S>, U extends List<T>> void withoutTrailingTypeParameter(
      Box<? super U> b) {}

  static void useWithoutTrailingTypeParameter(Box<List<List<@Odd String>>> b) {
    withoutTrailingTypeParameter(b);
  }
}
