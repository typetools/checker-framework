import checkers.nullness.quals.*;

public class Uninit10 {
  public Object f;
  Uninit10() {
    helper();
    assert f != null : "@SuppressWarnings(nullness)";
  }
  // for one reason or another, cannot be annotated with @AssertNonNullAfter
  void helper() {
    f = new Object();
  }

}
