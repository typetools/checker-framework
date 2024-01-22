import org.checkerframework.checker.nullness.qual.*;

public class SkipDirs1 {

  static class DontSkipMe {
    static Object foo() {
      // :: error: (return)
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
