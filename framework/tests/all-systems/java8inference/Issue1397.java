// Test case for Issue 1397.
// https://github.com/typetools/checker-framework/issues/1397

// @above-java17-skip-test TODO: reinstate on JDK 18, false positives are probably due to issue #979

public class Issue1397 {

  class Box<T> {}

  abstract class CrashCompound {
    abstract <T> T chk(T in);

    abstract <T> T unbox(Box<T> p);

    @SuppressWarnings("units")
    void foo(Box<Boolean> bb) {
      boolean res = false;
      res |= chk(unbox(bb));
    }
  }
}
