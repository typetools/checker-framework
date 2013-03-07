import checkers.nonnull.quals.EnsuresNonNull;
import checkers.nullness.quals.*;

public class AssertAfter {

  protected @Nullable String value;

  @EnsuresNonNull("value")
  public boolean setRepNonNull() {
    value = "";
    return true;
  }

  public void plain() {
    //:: error: (dereference.of.nullable)
    value.toString();
  }

  public void testAfter() {
    setRepNonNull();
    value.toString();
  }

  public void testBefore() {
    //:: error: (dereference.of.nullable)
    value.toString();
    setRepNonNull();
  }

  public void withCondition(@Nullable String t) {
    if (t == null) {
      setRepNonNull();
    }
    //:: error: (dereference.of.nullable)
    value.toString();
  }

  public void inConditionInTrue() {
    if (setRepNonNull()) {
      value.toString();
    } else { }
  }

  // skip-test: Come back when working on improved flow
  public void asCondition() {
      if (setRepNonNull()) {
      } else {
        value.toString(); // valid!
      }
  }
}
