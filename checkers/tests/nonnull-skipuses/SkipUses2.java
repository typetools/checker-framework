import checkers.nullness.quals.*;

public class SkipUses2 {

  static class SkipMe {
    static @Nullable Object f;
    @NonNullOnEntry("f")
    static void foo() { }
  }

  static class DontSkip {
    static @Nullable Object f;
    @NonNullOnEntry("f")
    static @Nullable Object foo() {
      return null;
    }
  }

  static class Main {
    void bar(boolean b) {
      SkipMe.f = null;
      SkipMe.foo();
      DontSkip.f = null;
      //:: error: (nonnullonentry.precondition.not.satisfied)
      DontSkip.foo();
    }
  }

}
