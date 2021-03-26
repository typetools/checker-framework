// Test case for Issue 1274
// https://github.com/typetools/checker-framework/issues/1274

@SuppressWarnings("all") // Just check for crashes
public class Issue1274 {
  static class Mine<T> {
    static <S> Mine<S> of(S p1, S p2) {
      return null;
    }
  }

  class Two<U, V> {}

  class C extends Two<Float, Float> {}

  class D extends Two<String, String> {}

  class Crash {
    {
      Mine.of(new C(), new D());
    }
  }
}
