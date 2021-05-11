import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/*
 * These tests ensure that EnsuresNonNullIf methods
 * are verified.
 */
public class EnsuresNonNullIfTestSimple {

  protected int @Nullable [] values;

  @EnsuresNonNullIf(result = true, expression = "values")
  public boolean repNulledBAD() {
    // :: error: (contracts.conditional.postcondition)
    return values == null;
  }

  @EnsuresNonNullIf(result = false, expression = "values")
  public boolean repNulled() {
    return values == null;
  }

  public void addAll(EnsuresNonNullIfTestSimple s) {
    if (repNulled()) {
      return;
    }
    @NonNull Object x = values;

    /* TODO skip-tests
     * The two errors are not raised currently
     * The assumption that "values" is NN is added above.
     * However, as repNulled is not pure, it should be removed again here.
    if (s.repNulled()) {
        // : : (dereference.of.nullable)
        values.hashCode();
    } else {
        // we called on "s", so we don't know anything about "values".
        // : : (assignment)
        @NonNull Object y = values;
    }
    */

    if (s.repNulled()) {
      // :: error: (dereference.of.nullable)
      s.values.hashCode();
    } else {
      @NonNull Object y = s.values;
    }
  }
}
