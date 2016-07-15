import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;

class MyGenClass<T extends @Nullable Object> {}

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.UPPER_BOUND)
class Varargs {
    void test() {
        ignore(newInstance());
    }

    static void ignore(MyGenClass<?>... consumer) {}

    static <T> MyGenClass<T> newInstance() {
        return new MyGenClass<T>();
    }
}
