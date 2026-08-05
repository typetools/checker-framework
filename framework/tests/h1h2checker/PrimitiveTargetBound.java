// Test for the third alternative of JLS 18.5.2.1: when the target type of an invocation is a
// primitive type and one of the primitive wrapper classes is a bound of the inference variable,
// the inference variable is resolved before the target type is taken into account.  The target
// type therefore does not refine the inferred type argument.

import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;

public class PrimitiveTargetBound {

  static <T extends @H1Top Integer> T get() {
    throw new AssertionError();
  }

  // The target type is primitive, so the type argument is resolved from the bounds of the type
  // variable alone: @H1Top Integer, which is not a subtype of the target type.
  void primitiveTarget() {
    // :: error: [assignment] :: error: [type.arguments.not.inferred]
    @H1S1 int x = get();
  }

  // The target type is a reference type, so it is used when resolving the type argument, which is
  // therefore @H1S1 Integer.
  void referenceTarget() {
    @H1S1 Integer y = get();
  }

  // The type argument resolved from the bounds alone is a subtype of the target type.
  void primitiveTargetWithinBound() {
    @H1Top int x = get();
  }
}
