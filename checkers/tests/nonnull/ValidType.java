import checkers.nonnull.quals.*;
import checkers.initialization.quals.*;

class ValidType {

    void t1() {
        //:: error: (type.invalid)
        @NonNull @Nullable String l1;
        //:: error: (type.invalid)
        @NonRaw @Raw @Free @Unclassified String f;
    }
}
