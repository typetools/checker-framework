import org.checkerframework.checker.nullness.qual.*;

public class SkipDefs1 {

  static class SkipMe {
    static Object foo() {
      return null;
    }
  }

  static class DontSkip {
    static Object foo() {
      // :: error: (return)
      return null;
    }
  }
}
