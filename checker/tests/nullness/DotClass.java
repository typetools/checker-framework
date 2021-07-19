import java.lang.annotation.Annotation;
import org.checkerframework.checker.nullness.qual.NonNull;

public class DotClass {

    void test() {
        doStuff(NonNull.class);
    }

    void doStuff(Class<? extends Annotation> cl) {}

    void access() {
        Object.class.toString();
        int.class.toString();
    }
}
