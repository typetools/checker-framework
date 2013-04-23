import checkers.initialization.quals.UnknownInitialization;
import checkers.nullness.quals.*;
class Raw2 {
  private @NonNull Object field;
  //:: error: (commitment.fields.uninitialized)
  public Raw2(int i) {
    this.method(this);
  }
  public Raw2() {
    try { this.method(this); }
    catch (NullPointerException e) { e.printStackTrace(); }
    field = 0L;
  }
  private void method(@Raw @UnknownInitialization Raw2 this, @Raw @UnknownInitialization Raw2 arg) {
    //:: error: (dereference.of.nullable)
    arg.field.hashCode();
    //:: error: (dereference.of.nullable)
    this.field.hashCode();
  }
  public static void test() {
    new Raw2();
  }
}
