import testlib.util.*;

// Test case for Issue 131:
// https://github.com/typetools/checker-framework/issues/131
public class GenericTest1 {
    public interface Foo<T> {}

    public interface Bar<T, C, E extends Foo<C>> extends Foo<T> {}

    public <T> void test(Foo<T> foo) {
        Bar<?, ?, ?> bar =
                foo instanceof Bar<?, ?, ?>
                        // TODO flow: support instanceof / cast flow.
                        // Warning only with -AcheckCastElementType.
                        // TODO:: warning: (cast.unsafe)
                        ? (Bar<?, ?, ?>) foo
                        : null;
    }
}
