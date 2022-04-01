import org.checkerframework.checker.signedness.qual.Signed;

public class SignednessNumberCasts {
    Double d2;

    void test(MyClass<?> o, MyClass<? extends Number> signed) {
        @Signed SignednessNumberCasts j = (SignednessNumberCasts) o.get();
        // :: error: (assignment.type.incompatible)
        @Signed int i = (Integer) o.get();
        @Signed int i2 = (Integer) signed.get();
        Double d = (Double) o.get();
        d2 = (Double) signed.get();
    }

    static interface MyClass<T> {
        T get();
    }
}
