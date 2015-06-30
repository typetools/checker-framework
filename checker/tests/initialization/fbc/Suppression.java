
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.*;

public class Suppression {

    @NonNull Suppression t;

    @SuppressWarnings("initialization.fields.uninitialized")
    public Suppression(Suppression arg) {

    }

    @SuppressWarnings({"fbc","nullness"})
    void foo(@UnknownInitialization Suppression arg) {
        t = arg;    // "fbc" error
        t = null;    // "nullness" error
    }

    void test() {
        @SuppressWarnings("nullness:assignment.type.incompatible")
        @NonNull String s = null;
    }

}
