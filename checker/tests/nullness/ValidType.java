import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class ValidType {

    void t1() {
        // :: error: (type.invalid.conflicting.annos)
        @NonNull @Nullable String l1;
        // :: error: (type.invalid.conflicting.annos)
        @NonRaw @Raw @UnderInitialization @UnknownInitialization String f;
    }
}
