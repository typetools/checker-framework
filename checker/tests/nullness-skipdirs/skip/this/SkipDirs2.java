import org.checkerframework.checker.nullness.qual.*;

public class SkipDirs2 {
  static class SkipMe {

    Object f;

    // If this test is NOT skipped, it should issue an "unexpected error" since
    // There is a type error between f2 (Nullable) and f (NonNull).
    void foo(@Nullable Object f2) {
      f = f2;
    }
  }
}
