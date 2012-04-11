import checkers.nullness.quals.*;

//@skip-test
class OverrideANNA2 {
  static class Super {
    Object f;

    @AssertNonNullAfter("f")
    void setf() {
      f = new Object();
    }

    Super() {
      setf();
    }
  }

  static class Sub extends Super {
    Object f;

    // TODO: some error here: the "f" in the annotation refers to a
    // different field than the superclass.
    @Override
    @AssertNonNullAfter("f")
    void setf() {
      f = new Object();
    }

    Sub() {
      setf();
    }
  }

  public static void main(String[] args) {
    Super s = new Sub();
    s.f.hashCode();
  }
}
