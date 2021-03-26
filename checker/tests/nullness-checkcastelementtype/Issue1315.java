// Test case for Issue 1315
// https://github.com/typetools/checker-framework/issues/1315

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1315 {
  static class Box<T> {
    T f;

    Box(T p) {
      f = p;
    }

    @SuppressWarnings("unchecked")
    T test1(@Nullable Object p) {
      // :: warning: (cast.unsafe)
      return (T) p;
    }
    // The Nullness Checker should not issue a cast.unsafe warning,
    // but the KeyFor Checker does, so suppress that warning.
    @SuppressWarnings({"unchecked", "keyfor:cast.unsafe"})
    T test2(Object p) {
      return (T) p;
    }
  }

  static class Casts {
    public static void test() {
      Box<String> bs = new Box<>("");
      bs.f = bs.test1(null);
      // :: error: (argument.type.incompatible)
      bs.f = bs.test2(null);
      bs.f.toString();
    }
  }
}
