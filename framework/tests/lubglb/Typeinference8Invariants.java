import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

// This file contains no test code of its own.  The actual tests are performed by
// Typeinference8InvariantTests, which builds inference types and constraints out of the
// declarations below and then checks that violating an invariant of package
// org.checkerframework.framework.util.typeinference8 fails loudly.
//
// The declarations are here, rather than being synthesized by the test code, because building an
// AnnotatedTypeMirror requires a real declaration to read it from.
public class Typeinference8Invariants {

  /** Used to create a proper type whose type argument can be replaced by an inference variable. */
  Holder<String> holder;

  /**
   * Used only as the key under which a {@code Theta} is cached; never reduced. It must be a
   * different lambda than the ones that are reduced, because {@code
   * InferenceFactory.createThetaForLambda} caches one {@code Theta} per lambda tree.
   */
  Runnable thetaKeyLambda = () -> {};

  /** An explicitly typed lambda with one parameter. */
  Function<String, String> oneParameterLambda = (String s) -> s;

  /** An explicitly typed lambda with three parameters. */
  ThreeParameters threeParameterLambda = (String a, String b, String c) -> a;

  /** A functional interface whose function type has three parameters. */
  interface ThreeParameters {
    String apply(String a, String b, String c);
  }

  /** Declares types that mention a type variable, so that they can be made into inference types. */
  static class Holder<Z> {

    /** A functional interface type, mentioning {@code Z}, whose function type has no parameters. */
    Supplier<Z> supplierField;

    /**
     * A wildcard-parameterized functional interface type, mentioning {@code Z}, whose function type
     * has two parameters.
     */
    BiFunction<?, ?, Z> biFunctionField;

    /** A declared (not array) type that mentions {@code Z}. */
    List<Z> listField;
  }
}
