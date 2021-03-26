import org.checkerframework.checker.nullness.qual.*;

public class SkipDefs2 {

  static class SkipMe {
    @Nullable Object f;

    @EnsuresNonNull("f")
    static void foo() {}
  }

  static class DontSkip {
    static Object foo() {
      // :: error: (return.type.incompatible)
      return null;
    }
  }
}
