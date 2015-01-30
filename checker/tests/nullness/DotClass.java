import org.checkerframework.checker.nullness.qual.*;
import java.lang.annotation.*;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)
class DotClass {

    void test() {
        doStuff(NonNull.class);
    }

    void doStuff(Class<? extends Annotation> cl) { }

    void access() {
        Object.class.toString();
    }
}
