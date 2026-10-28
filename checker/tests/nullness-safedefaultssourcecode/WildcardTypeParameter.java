// Test that the annotations on an unbounded wildcard come from the type parameter that the wildcard
// instantiates.  javac records that type parameter in the wildcard, but javac overwrites the record
// whenever it substitutes the wildcard for a type variable.  Checking `WildcardTypeParameterSub1`
// substitutes the wildcards of the `WildcardTypeParameterSuper` constructor's parameter type for
// the type parameter of `WildcardTypeParameterAnalysis`, whose upper bound is `@NonNull`.  If the
// Checker Framework used the overwritten record, then the type of that parameter would differ
// between `WildcardTypeParameterSub1` and `WildcardTypeParameterSub2`, and the call to `super` in
// `WildcardTypeParameterSub2` would be rejected.

import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
class WildcardTypeParameterSub1 extends WildcardTypeParameterSuper {
  WildcardTypeParameterSub1(WildcardTypeParameterFactory<?, ?> factory) {
    super(factory);
  }
}

@AnnotatedFor("nullness")
class WildcardTypeParameterSub2 extends WildcardTypeParameterSuper {
  WildcardTypeParameterSub2(WildcardTypeParameterFactory<?, ?> factory) {
    super(factory);
  }
}

@AnnotatedFor("nullness")
abstract class WildcardTypeParameterSuper {
  WildcardTypeParameterSuper(WildcardTypeParameterFactory<?, ?> factory) {}
}

// This class is not annotated for the Nullness Checker, so the upper bounds of its type parameters
// are `@Nullable`.
class WildcardTypeParameterFactory<
    FV extends Comparable<FV>, FA extends WildcardTypeParameterAnalysis<FV>> {}

@AnnotatedFor("nullness")
class WildcardTypeParameterAnalysis<AV extends Comparable<AV>>
    extends WildcardTypeParameterBase<AV> {}

@AnnotatedFor("nullness")
class WildcardTypeParameterBase<BV extends Comparable<BV>> {}
