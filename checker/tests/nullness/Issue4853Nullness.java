import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue4853Nullness {
    interface Interface<Q> {}

    static class MyClass<T> {
        class InnerMyClass implements Interface<T> {}
    }

    abstract static class SubMyClass extends MyClass<@Nullable String> {
        protected void f() {
            // :: error: (argument.type.incompatible)
            method(new InnerMyClass());
        }

        abstract void method(Interface<String> callback);
    }
}
