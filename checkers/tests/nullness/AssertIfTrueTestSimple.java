import checkers.nullness.quals.*;

/*
 * These tests ensure that AssertNonNullIfTrue methods
 * are verified.
 */
public class AssertIfTrueTestSimple {

  protected int @Nullable [] values;

  @AssertNonNullIfTrue("values")
  public boolean repNulledBAD() {
    //:: error: (assertiftrue.postcondition.not.satisfied)
    return values == null;
  }

  @AssertNonNullIfFalse("values")
  public boolean repNulled() {
    return values == null;
  }

  public void addAll(AssertIfTrueTestSimple s) {
    if (repNulled())
      return;
    @NonNull Object x = values;

    /* TODO skip-tests
     * The two errors are not raised currently
     * The assumption that "values" is NN is added above.
     * However, as repNulled is not pure, it should be removed again here.
    if (s.repNulled()) {
        // :: (dereference.of.nullable)
        values.hashCode();
    } else {
        // we called on "s", so we don't know anything about "values".
        // :: (assignment.type.incompatible)
        @NonNull Object y = values;
    }
    */
    
    if (s.repNulled()) {
        //:: error: (dereference.of.nullable)
        s.values.hashCode();
    } else {
        @NonNull Object y = s.values;
    }
  }

}
