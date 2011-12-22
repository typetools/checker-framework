import checkers.nullness.quals.*;

class OverrideNNOE {
  static class Super {
    @Nullable Object f;

    @NonNullOnEntry("f")
    void call() {}
  }

  static class Sub extends Super {
    @Nullable Object g;

    @Override
    @NonNullOnEntry({"f", "g"})
    //:: error: (override.pre.method.annotation.part.invalid)
    void call() {
      g.hashCode();
    }
  }

  public static void main(String[] args) {
    Super s = new Sub();
    s.f = new Object();
    s.call();
  }
}
