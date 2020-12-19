import org.checkerframework.checker.nullness.qual.*;

public class RawTypesGenerics {
    void m() throws ClassNotFoundException {
        Class c1 = Class.forName("bla");
        Class<? extends @Nullable Object> c2 = Class.forName("bla");
    }

    class Test<X extends Number> {}

    void bar() {
        // Java will complain about this:
        // Test x = new Test<Object>();

        // ok
        Test y = new Test<Integer>();

        // :: error: (type.argument.type.incompatible)
        Test z = new Test<@Nullable Integer>();
    }

    void m(java.lang.reflect.Constructor<?> c) {
        Class cls1 = c.getParameterTypes()[0];
        Class<? extends @Nullable Object> cls2 = c.getParameterTypes()[0];
    }
}
