
public class FieldAccess {
    class MyClass {
        Object field = new Object();
    }

    class MyException extends RuntimeException {
        Object field = new Object();
    }

    class MyExceptionA extends MyException {}

    class MyExceptionB extends MyException {}

    @SuppressWarnings("nullness")
    class MyGen<T extends MyClass> {
        T myClass = null;
    }

    void test(Object o, MyGen raw) {
        // Raw type field access:
        raw.myClass.field = new Object();

        // Intersection type field access
        Object a = ((MyClass & Cloneable) o).field;
        try {
        } catch (MyExceptionA | MyExceptionB ex) {
            // Union type field access
            ex.field = new Object();
        }
    }
}
