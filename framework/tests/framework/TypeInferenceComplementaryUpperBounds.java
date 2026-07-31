// Test for the last paragraph of JLS 18.3.1: when a bound set contains a pair of bounds
// `var <: S` and `var <: T`, where a supertype of S is `G<S1, ..., Sn>` and a supertype of T
// is `G<T1, ..., Tn>`, then the constraint formula <Si = Ti> is implied for each i.
//
// Incorporation used to pair the new *upper* bound against the existing *lower* bounds, so this
// rule never fired.  Without it, `S` below is constrained only by `Object`, so the implicit upper
// bound `T <: List<S>` becomes `List<Object>`, which is not a supertype of `List<@Odd String>`,
// and inference reports a spurious `type.arguments.not.inferred` error.

import java.util.List;
import org.checkerframework.framework.testchecker.util.Odd;

public class TypeInferenceComplementaryUpperBounds {

  static class Box<X> {}

  // The two upper bounds on T are `List<S>` (from the declaration of T) and
  // `List<@Odd String>` (from the argument, via the `? super T` wildcard).
  // 18.3.1 implies <S = @Odd String>.
  static <S, T extends List<S>> void twoUpperBounds(Box<? super T> b) {}

  static void useTwoUpperBounds(Box<List<@Odd String>> b) {
    twoUpperBounds(b);
  }

  // The same, one level deeper: 18.3.1 must fire for U to give T, then for T to give S.
  static <S, T extends List<S>, U extends List<T>> void nestedUpperBounds(Box<? super U> b) {}

  static void useNestedUpperBounds(Box<List<List<@Odd String>>> b) {
    nestedUpperBounds(b);
  }

  // The implied constraint is an equality, so it also rules out an argument that would otherwise
  // be consistent with the remaining bounds.
  static <S, T extends List<S>> void twoUpperBoundsAndArg(Box<? super T> b, List<S> l) {}

  static void useTwoUpperBoundsAndArgOdd(Box<List<@Odd String>> b, List<@Odd String> l) {
    twoUpperBoundsAndArg(b, l);
  }

  static void useTwoUpperBoundsAndArgMismatch(Box<List<@Odd String>> b, List<String> l) {
    // 18.3.1 implies <S = @Odd String>, which `l` does not satisfy.
    // :: error: (type.arguments.not.inferred)
    twoUpperBoundsAndArg(b, l);
  }
}
