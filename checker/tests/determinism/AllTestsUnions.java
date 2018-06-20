package determinism;

public class AllTestsUnions {
    void foo1(MyInterface<Throwable> param) throws Throwable {
        try {
            bar();
        } catch (MyExceptionA | MyExceptionB ex) {
            // ::error: (type.argument.type.incompatible)
            typeVar(ex);
            // ::error: (type.argument.type.incompatible)
            typeVarIntersection(ex);

            // ::error: (type.argument.type.incompatible)
            typeVarWildcard(ex, param);
        }
    }

    void foo2(MyInterface<Throwable> param) throws Throwable {
        try {
            bar();
        } catch (SubMyExceptionA | SubMyExceptionA2 ex) {
            // ::error: (type.argument.type.incompatible)
            typeVar(ex);
            // ::error: (type.argument.type.incompatible)
            typeVar2(ex, ex);

            // ::error: (type.argument.type.incompatible)
            typeVarIntersection(ex);

            // ::error: (type.argument.type.incompatible)
            typeVarWildcard(ex, param);
        }
    }

    <T extends Cloneable & MyInterface<String>> void typeVarIntersection(T param) {}

    <T extends Throwable> void typeVar(T param) {}

    <T extends Throwable> void typeVar2(T param, T param2) {}

    <T extends Throwable> void typeVarWildcard(T param, MyInterface<? extends T> myInterface) {}

    <T extends Throwable> void typeVarWildcard2(T param, MyInterface<? super T> myInterface) {}

    void bar() throws MyExceptionA, MyExceptionB {}

    interface MyInterface<T> {}

    class MyExceptionA extends Throwable implements Cloneable, MyInterface<String> {}

    class MyExceptionB extends Throwable implements Cloneable, MyInterface<String> {}

    class SubMyExceptionA extends MyExceptionA {}

    class SubMyExceptionA2 extends MyExceptionA {}
}
