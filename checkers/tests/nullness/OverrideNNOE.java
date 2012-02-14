import checkers.nullness.quals.*;

class OverrideNNOE {
  static class Super {
    @Nullable Object f;

    void call() {}
  }

  static class Sub extends Super {
    @Override
    @NonNullOnEntry("f")
    //:: error: (override.pre.method.annotation.invalid)
    void call() {
      f.hashCode();
    }
  }

  public static void main(String[] args) {
    Super s = new Sub();
    s.call();
  }
}
