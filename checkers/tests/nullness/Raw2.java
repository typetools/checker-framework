import checkers.nullness.quals.*;
class Raw2 {
  private @NonNull Object field;
  //:: error: (fields.uninitialized)
  public Raw2(int i) {
    this.method(this);
  }
  public Raw2() {
    try { this.method(this); }
    catch (NullPointerException e) { e.printStackTrace(); }
    field = 0L;
  }
  private void method(@Raw Raw2 this, @Raw Raw2 arg) {
    //:: error: (dereference.of.nullable)
    arg.field.hashCode();
    // I presume that this gives no warning because of the checkers'
    // built-in heuristic that any given warning should be issued just
    // once.  However, since the field is being looked up on a different
    // object ("this" vs. "arg", it should arguably be output regardless of
    // the heuristic.
    // TODO: //:: error: (dereference.of.nullable)
    this.field.hashCode();
  }
  public static void test() {
    new Raw2();
  }
}

