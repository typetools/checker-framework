import org.checkerframework.checker.nullness.qual.*;

public class ThreadLocalTest {

    // implementation MUST override initialValue(), or SuppressWarnings is unsound
    @SuppressWarnings("nullness:type.argument.type.incompatible")
    class MyThreadLocalNN extends ThreadLocal<@NonNull Integer> {
        @Override
        protected Integer initialValue() {
            return new Integer(0);
        }
    }

    void foo() {
        // :: error: (type.argument.type.incompatible)
        new ThreadLocal<@NonNull Object>();
        // :: error: (type.argument.type.incompatible)
        new InheritableThreadLocal<@NonNull Object>();
        new ThreadLocal<@Nullable Object>();
        new InheritableThreadLocal<@Nullable Object>();
        new MyThreadLocalNN();
    }
}
