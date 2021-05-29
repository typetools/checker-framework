// Test case for issue #1572 and issue #1330
// https://github.com/typetools/checker-framework/issues/1572
// https://github.com/typetools/checker-framework/issues/1330

// @skip-test until the issues are fixed

import org.checkerframework.checker.nullness.qual.*;

public class ThreadLocalTest2 {

  private static int unwrap(final ThreadLocal<Integer> tl) {
    return tl.get().intValue();
  }

  private static void wrap(final ThreadLocal<Integer> tl, final int value) {
    tl.set(Integer.valueOf(value));
  }

  private static ThreadLocal<Integer> consumed_chars =
      new ThreadLocal<Integer>() {

        @Override
        protected Integer initialValue() {
          return Integer.valueOf(0);
        }
      };

  class MyThreadLocalNN extends ThreadLocal<@NonNull Integer> {
    @Override
    protected Integer initialValue() {
      return new Integer(0);
    }
  }

  class MyThreadLocalNnIncorrectOverride extends ThreadLocal<@NonNull Integer> {
    @Override
    // :: error: (override.return)
    protected @Nullable Integer initialValue() {
      return null;
    }
  }

  // :: error: (method.not.overridden)
  class MyThreadLocalNnNoOverride extends ThreadLocal<@NonNull Integer> {}

  class MyThreadLocalNble extends ThreadLocal<@Nullable Integer> {
    @Override
    protected @Nullable Integer initialValue() {
      return null;
    }
  }

  class MyThreadLocalNbleStrongerOverride extends ThreadLocal<@Nullable Integer> {
    @Override
    protected @NonNull Integer initialValue() {
      return new Integer(0);
    }
  }

  class MyThreadLocalNbleNoOverride extends ThreadLocal<@Nullable Integer> {}

  void foo() {
    // :: error: (type.argument)
    new ThreadLocal<@NonNull Object>();
    // :: error: (type.argument)
    new InheritableThreadLocal<@NonNull Object>();
    new ThreadLocal<@Nullable Object>();
    new InheritableThreadLocal<@Nullable Object>();
    new MyThreadLocalNN();
  }
}
