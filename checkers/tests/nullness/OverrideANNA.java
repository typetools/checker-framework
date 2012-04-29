import checkers.nullness.quals.*;

class OverrideANNA {
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
    @Override
    //:: error: (override.post.method.annotation.invalid)
    void setf() { }
  }

  public static void main(String[] args) {
    Super s = new Sub();
    s.f.hashCode();
  }
}
