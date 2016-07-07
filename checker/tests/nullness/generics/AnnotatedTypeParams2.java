import java.util.*;
import org.checkerframework.checker.nullness.qual.*;

class MyClass<@Nullable T> {
    T get() {
        throw new RuntimeException();
    }
}

class OtherClass {

    void testPositive() {
        MyClass<@Nullable String> l = new MyClass<@Nullable String>();
        //:: error: (dereference.of.nullable)
        l.get().toString();
    }

    void testInvalidParam() {
        //:: error: (type.argument.type.incompatible)
        MyClass<@NonNull String> l;
    }
}
