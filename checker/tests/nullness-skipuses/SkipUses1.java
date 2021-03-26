import org.checkerframework.checker.nullness.qual.*;

public class SkipUses1 {

  static class SkipMe {
    static @Nullable Object foo() {
      return null;
    }
  }

  static class DontSkip {
    static @Nullable Object foo() {
      return null;
    }
  }

  static class Main {
    void bar(boolean b) {
      @NonNull Object x = SkipMe.foo();
      // :: error: (assignment.type.incompatible)
      @NonNull Object y = DontSkip.foo();

      // :: error: (assignment.type.incompatible)
      @NonNull Object z = b ? SkipMe.foo() : DontSkip.foo();
    }
  }
}
