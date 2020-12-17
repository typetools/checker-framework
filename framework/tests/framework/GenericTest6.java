import org.checkerframework.framework.testchecker.util.*;

// Test case for Issue 136:
// https://github.com/typetools/checker-framework/issues/136
public class GenericTest6 {
    interface Foo<T extends Foo<?>> {}

    class Strange implements Foo<Strange> {}

    void test(Foo<Strange> p) {}

    void call(Foo<Strange> p) {
        test(p);
    }

    void test2(Foo<Foo<?>> p) {}

    void call2(Foo<Foo<?>> p) {
        test2(p);
    }

    void test3(Foo<Foo<? extends @Odd Foo<?>>> p) {}

    void call3(Foo<Foo<? extends Foo<?>>> p) {
        // :: error: (argument.type.incompatible)
        test3(p);
    }

    void testRaw(Foo p) {}

    void callRaw(Foo<Foo<?>> p) {
        testRaw(p);
    }
}
