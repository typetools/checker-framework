import checkers.nullness.quals.AssertNonNullAfter;

public class Uninit8 {

  Object f;

  Uninit8() {
    setFields();
    f.toString();
  }

  @AssertNonNullAfter("f")
  void setFields() {
    f = new Object();
  }

}
