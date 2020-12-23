public class Unboxing {
    boolean b = Boolean.TRUE;

    <T> T foo(Class<T> expectedType) {
        return null;
    }

    boolean b2 = foo(Boolean.class);
    boolean b3 = foo(null);

    <T> T bar() {
        return null;
    }

    boolean b4 = bar();
}
