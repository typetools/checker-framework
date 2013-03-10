import checkers.nullness.quals.*;

class OverrideANNA {
  static class Super {
    Object f;
    Object g;

    @AssertNonNullAfter({"f", "g"})
    void setfg() {
      f = new Object();
      g = new Object();
    }

    Super() {
      setfg();
    }
  }

  static class Sub extends Super {
    @Override
    @AssertNonNullAfter("f")
    //:: error: (override.post.method.annotation.part.invalid)
    void setfg() {
      f = new Object();
    }
  }

  public static void main(String[] args) {
    Super s = new Sub();
    s.g.hashCode();
  }
}
