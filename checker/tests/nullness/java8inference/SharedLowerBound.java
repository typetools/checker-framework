// Resolution must not replace the annotations of an `AnnotatedTypeMirror` that a bound set holds.
// `InferenceFactory.lub(Set)` returns a type that shares its `AnnotatedTypeMirror` with its
// argument when the argument has one element, and that annotated type may also be the
// instantiation of a variable that was resolved earlier.
//
// Below, `B` depends on `A`, so `A` is resolved first:  `A`'s only proper lower bound is
// `@NonNull String`, and resolution instantiates `A` to that very annotated type.  Applying the
// instantiation makes it `B`'s only proper lower bound as well.  Resolving `B` takes the lub of
// `B`'s proper lower bounds -- that same annotated type -- and raises it to `@Nullable`, because
// the `null` argument gives `B` the qualifier lower bound `@Nullable`.  If resolution replaced
// those annotations in place instead of copying first, it would also change `A`'s instantiation to
// `@Nullable String`, contradicting `A`'s upper bound `@NonNull String` from the `List<? super A>`
// parameter.  Incorporating that pair of bounds yields `false`, so inference would report
// `type.arguments.not.inferred` for the well-typed call in `use`.

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SharedLowerBound {

  static <A> A first(A a, List<? super A> l) {
    return a;
  }

  static <B> B second(B b, B c) {
    return b;
  }

  void use(@NonNull String s, List<@NonNull String> l) {
    second(first(s, l), null);
  }

  // `B`'s qualifier lower bound still constrains `B` itself:  `B` is `@Nullable String`, not
  // `@NonNull String`.
  void resultIsNullable(@NonNull String s, List<@NonNull String> l) {
    @Nullable String ok = second(first(s, l), null);
    // :: error: [assignment] :: error: [type.arguments.not.inferred]
    @NonNull String bad = second(first(s, l), null);
  }
}
