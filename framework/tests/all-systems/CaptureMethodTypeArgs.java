package wildcards;

@SuppressWarnings("all") // Just check for crashes.
public class CaptureMethodTypeArgs<T> {
    static class MyClass {}

    private MyClass myClass;

    void test(Class<T> cls) {
        Object o = method(cls.getComponentType()).myClass;
    }

    static <T> CaptureMethodTypeArgs<? extends T> method(Class<T> cls) {
        throw new RuntimeException();
    }
}
