import checkers.nullness.quals.*;
import java.lang.annotation.*;

@checkers.quals.DefaultQualifier(NonNull.class)
class DotClass {

    void test() {
        doStuff(NonNull.class);
    }

    void doStuff(Class<? extends Annotation> cl) { }

    void access() {
        Object.class.toString();
    }
}
