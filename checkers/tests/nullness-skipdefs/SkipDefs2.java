import checkers.nullness.quals.*;

public class SkipDefs2 {

  static class SkipMe {
    @Nullable Object f;
    @AssertNonNullAfter("f")
    static void foo() {
    }
  }

  static class DontSkip {
    static Object foo() {
      //:: error: (return.type.incompatible)
      return null;
    }
  }

}
