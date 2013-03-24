import checkers.initialization.quals.Raw;
import checkers.initialization.quals.Unclassified;
import checkers.nullness.quals.EnsuresNonNull;
import checkers.nullness.quals.*;

class OverrideANNA {
  static class Super {
    Object f;

    @EnsuresNonNull("f")
    void setf(@Raw @Unclassified Super this) {
      f = new Object();
    }

    Super() {
      setf();
    }
  }

  static class Sub extends Super {
    @Override
    //:: error: (contracts.postcondition.override.invalid)
    void setf(@Raw @Unclassified Sub this) { }
  }

  public static void main(String[] args) {
    Super s = new Sub();
    s.f.hashCode();
  }
}
