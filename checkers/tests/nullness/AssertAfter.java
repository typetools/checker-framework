import checkers.nullness.quals.*;

public class AssertAfter {

  protected @Nullable String value;

  @AssertNonNullAfter("value")
  @Pure
  public boolean repNulled() {
    return value == null;
  }

  public void plain() {
      //:: (dereference.of.nullable)
      value.toString();
  }

  public void testAfter() {
      repNulled();
      value.toString();
  }

  public void testBefore() {
      //:: (dereference.of.nullable)
      value.toString();
      repNulled();
  }

  public void withCondition() {
      if (toString() == null) {
          repNulled();
      }
      //:: (dereference.of.nullable)
      value.toString();
  }

  // skip-test: Come back when working on improved flow
//  public void asCondition() {
//      if (repNulled()) {
//      } else {
//          value.toString(); valid!
//      }
//  }
}
