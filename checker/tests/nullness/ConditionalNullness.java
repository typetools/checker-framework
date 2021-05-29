import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

public class ConditionalNullness {

  @EnsuresNonNullIf(
      expression = {"field", "method()"},
      result = true)
  boolean checkNonNull() {
    // don't bother with the implementation
    // :: error: (contracts.conditional.postcondition)
    return true;
  }

  @Nullable Object field = null;

  @org.checkerframework.dataflow.qual.Pure
  @Nullable Object method() {
    return "m";
  }

  void testSelfWithCheck() {
    ConditionalNullness other = new ConditionalNullness();
    if (checkNonNull()) {
      field.toString();
      method().toString();
      // :: error: (dereference.of.nullable)
      other.field.toString(); // error
      // :: error: (dereference.of.nullable)
      other.method().toString(); // error
    }
    // :: error: (dereference.of.nullable)
    method().toString(); // error
  }

  void testSelfWithoutCheck() {
    // :: error: (dereference.of.nullable)
    field.toString(); // error
    // :: error: (dereference.of.nullable)
    method().toString(); // error
  }

  void testSelfWithCheckNegation() {
    if (checkNonNull()) {
      // nothing to do
    } else {
      // :: error: (dereference.of.nullable)
      field.toString(); // error
    }
    field.toString(); // error
  }

  void testOtherWithCheck() {
    ConditionalNullness other = new ConditionalNullness();
    if (other.checkNonNull()) {
      other.field.toString();
      other.method().toString();
      // :: error: (dereference.of.nullable)
      field.toString(); // error
      // :: error: (dereference.of.nullable)
      method().toString(); // error
    }
    // :: error: (dereference.of.nullable)
    other.method().toString(); // error
    // :: error: (dereference.of.nullable)
    method().toString(); // error
  }

  void testOtherWithoutCheck() {
    ConditionalNullness other = new ConditionalNullness();
    // :: error: (dereference.of.nullable)
    other.field.toString(); // error
    // :: error: (dereference.of.nullable)
    other.method().toString(); // error
    // :: error: (dereference.of.nullable)
    field.toString(); // error
    // :: error: (dereference.of.nullable)
    method().toString(); // error
  }

  void testOtherWithCheckNegation() {
    ConditionalNullness other = new ConditionalNullness();
    if (other.checkNonNull()) {
      // nothing to do
    } else {
      // :: error: (dereference.of.nullable)
      other.field.toString(); // error
      // :: error: (dereference.of.nullable)
      other.method().toString(); // error
      // :: error: (dereference.of.nullable)
      field.toString(); // error
    }
    // :: error: (dereference.of.nullable)
    field.toString(); // error
  }
}
