// @skip-test
public class UnionCrash {
    void foo(MyInterface<Throwable> param) throws Throwable {
        try {
            bar();
        } catch (MyExceptionA | MyExceptionB ex1) {
            try {
                bar();
            } catch (SubMyExceptionA | MyExceptionB ex2) {
                typeVar(ex1, ex2);
            }
        }
    }

    <T extends Throwable> void typeVar(T param, T param2) {
    }

    void bar() throws MyExceptionA, MyExceptionB {
    }

    interface MyInterface<T> {
    }

    class MyExceptionA extends Throwable implements Cloneable, MyInterface<String> {
    }

    class MyExceptionB extends Throwable implements Cloneable, MyInterface<String> {
    }

    class SubMyExceptionA extends MyExceptionA {
    }
}
