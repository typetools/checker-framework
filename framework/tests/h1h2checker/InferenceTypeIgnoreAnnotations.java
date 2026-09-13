import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

/**
 * When a use of an inference variable has a primary annotation, the bounds created for that use are
 * marked as having annotations that should be ignored. {@code InferenceType.isSubType} honors that
 * marking, via {@code AbstractType.compareAnnotations}.
 *
 * <p>The annotations are ignored if either the subtype or the supertype is marked, so this file
 * tests both. In the first two methods below, the marked type is the subtype {@code MyList<U>}; the
 * two methods infer the same type argument and issue the same two errors. In {@code
 * markedSuperType}, the marked type is the supertype instead.
 *
 * <p>If {@code InferenceType.isSubType} did not honor the marking, then the bound {@code MyList<U>
 * <: @H2S2 Foo} would be annotation-checked in the H2 hierarchy and would therefore be
 * unsatisfiable. Then {@code inferenceTypeLowerBound} would issue {@code
 * type.arguments.not.inferred} in place of its two errors, and {@code markedSuperType} would issue
 * {@code type.arguments.not.inferred} in addition to its one error.
 */
public class InferenceTypeIgnoreAnnotations {

  interface Foo {}

  static class MyList<E> implements Foo {}

  static @H2S1 MyList<String> makeProper() {
    throw new RuntimeException();
  }

  static <U> @H2S1 MyList<U> makeInference(U u) {
    throw new RuntimeException();
  }

  // The use of `T` as the type of the formal parameter has a primary annotation, and that primary
  // annotation is in the H1 hierarchy only.
  static <T extends @H2S2 Foo> void take(@H1S1 T t) {}

  // The lower bound of the inference variable for `T` is a ProperType.
  void properLowerBound() {
    // :: error: [type.argument] :: error: [argument]
    take(makeProper());
  }

  // The lower bound of the inference variable for `T` is an InferenceType, because it mentions the
  // inference variable for `U`.
  void inferenceTypeLowerBound(String s) {
    // :: error: [type.argument] :: error: [argument]
    take(makeInference(s));
  }

  // The use of `T` as the return type has a primary annotation, and that primary annotation is in
  // the H1 hierarchy only.  The use of `T` as the type of the formal parameter has no primary
  // annotation.  `T`'s declared upper bound has no explicit annotation, so it defaults to the top
  // qualifier in each hierarchy and by itself forbids no argument.
  static <T extends Foo> @H1S1 T mix(T t) {
    throw new RuntimeException();
  }

  // The marked type is the supertype rather than the subtype: the assignment context supplies the
  // upper bound `@H2S2 Foo`, which is marked because the use of `T` as the return type has a
  // primary annotation.  The lower bound `MyList<U>` is an unmarked InferenceType.
  void markedSuperType(String s) {
    // :: error: [assignment]
    @H2S2 Foo f = mix(makeInference(s));
  }
}
