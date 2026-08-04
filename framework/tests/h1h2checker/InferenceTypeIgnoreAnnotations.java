import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

/**
 * When a use of an inference variable has a primary annotation, the bounds created for that use are
 * marked as having annotations that should be ignored. {@code InferenceType.isSubType} must honor
 * that marking, just as the three analogous methods in {@code ProperType} do.
 *
 * <p>Both methods below infer the same type argument and issue the same two errors. Before {@code
 * InferenceType.isSubType} honored the marking, the second method instead issued {@code
 * type.arguments.not.inferred}, because the bound {@code MyList<U> <: @H2S2 Foo} was
 * annotation-checked in the H2 hierarchy.
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
}
