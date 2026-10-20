// Test for the third alternative of JLS 18.5.2.1: when the target type of an invocation is a
// primitive type and one of the primitive wrapper classes is a bound of the inference variable,
// the inference variable is resolved before the target type is taken into account.  The target
// type therefore does not refine the inferred type argument, and the result of the invocation may
// be null.

import org.checkerframework.checker.nullness.qual.Nullable;

public class PrimitiveTargetBound {

  static <T extends @Nullable Integer> T getInteger() {
    throw new AssertionError();
  }

  static <T extends @Nullable Boolean> T getBoolean() {
    throw new AssertionError();
  }

  static <T extends @Nullable Character> T getCharacter() {
    throw new AssertionError();
  }

  // In each of the following methods, the type argument is resolved from the bounds of the type
  // variable alone, so it is @Nullable and unboxing it is unsafe.

  void intTarget() {
    // :: error: [type.arguments.not.inferred] :: error: [unboxing.of.nullable]
    int x = getInteger();
  }

  void longTarget() {
    // :: error: [type.arguments.not.inferred] :: error: [unboxing.of.nullable]
    long x = getInteger();
  }

  void doubleTarget() {
    // :: error: [type.arguments.not.inferred] :: error: [unboxing.of.nullable]
    double x = getInteger();
  }

  void booleanTarget() {
    // :: error: [type.arguments.not.inferred] :: error: [unboxing.of.nullable]
    boolean x = getBoolean();
  }

  void charTarget() {
    // :: error: [type.arguments.not.inferred] :: error: [unboxing.of.nullable]
    char x = getCharacter();
  }

  // By contrast, a reference target type is used when resolving the type argument, so the type
  // argument is @NonNull Integer and there is no error.

  void referenceTarget() {
    Integer y = getInteger();
  }

  void nullableReferenceTarget() {
    @Nullable Integer z = getInteger();
  }
}
