package determinism;

// Test case for issue #775

public class AllTestsUnionCrash {
    void foo(MyInterface<Throwable> param) throws Throwable {
        try {
            bar();
        } catch (MyExceptionA | MyExceptionB ex1) {
            try {
                bar();
            } catch (SubMyExceptionA | MyExceptionB ex2) {
                // This call cause a crash
                // ::error: (type.argument.type.incompatible)
                typeVar(ex1, ex2);
            }
        }
    }

    <T extends Throwable> void typeVar(T param, T param2) {}

    void bar() throws MyExceptionA, MyExceptionB {}

    interface MyInterface<T> {}

    class MyExceptionA extends Throwable implements Cloneable, MyInterface<String> {}

    class MyExceptionB extends Throwable implements Cloneable, MyInterface<String> {}

    class SubMyExceptionA extends MyExceptionA {}
}
