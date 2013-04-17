import checkers.initialization.quals.*;
import checkers.nullness.quals.*;

class OverrideANNA2 {
  static class Super {
    Object f;

    @EnsuresNonNull("f")
    void setf(@Raw @UnkownInitialization Super this) {
      f = new Object();
    }

    Super() {
      setf();
    }
  }

  static class Sub extends Super {
    Object f;

    @Override
    @EnsuresNonNull("f")
    //:: error: (contracts.postcondition.override.invalid)
    void setf(@Raw @UnkownInitialization Sub this) {
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
