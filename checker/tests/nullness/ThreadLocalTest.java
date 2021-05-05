import org.checkerframework.checker.nullness.qual.*;

public class ThreadLocalTest {

  // implementation MUST override initialValue(), or SuppressWarnings is unsound
  @SuppressWarnings("nullness:type.argument")
  class MyThreadLocalNN extends ThreadLocal<@NonNull Integer> {
    @Override
    protected Integer initialValue() {
      return Integer.valueOf(0);
    }
  }

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
