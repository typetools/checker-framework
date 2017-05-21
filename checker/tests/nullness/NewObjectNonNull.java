import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;

class NewObjectNonNull {
    @DefaultQualifier(Nullable.class)
    class A {
        A() {}
    }

    void m() {
        new A().toString();
    }
}
