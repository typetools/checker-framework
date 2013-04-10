import checkers.initialization.quals.UnkownInitialization;
import checkers.nullness.quals.EnsuresNonNull;
import checkers.nullness.quals.Raw;

class OverrideANNA {
  static class Super {
    Object f;
    Object g;

    @EnsuresNonNull({"f", "g"})
    void setfg(@Raw @UnkownInitialization Super this) {
      f = new Object();
      g = new Object();
    }

    Super() {
      setfg();
    }
  }

  static class Sub extends Super {
    @Override
    @EnsuresNonNull("f")
    //:: error: (contracts.postcondition.override.invalid)
    void setfg(@Raw @UnkownInitialization Sub this) {
      f = new Object();
    }
  }

  public static void main(String[] args) {
    Super s = new Sub();
    s.g.hashCode();
  }
}
