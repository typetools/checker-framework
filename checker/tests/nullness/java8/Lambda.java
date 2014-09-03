
import org.checkerframework.checker.nullness.qual.*;
// @skip-test We need to implement this checking

class Lambda {

    void context() {
        Func func = in -> {
            //:: error: (assignment.type.incompatible)
            in.toString();
            //:: error: (return.type.incompatible)
            return in;
        };

        //:: error: (return.type.incompatible) ?
        Func func2 = in -> null;
    }

    interface Func {
        @NonNull String method(@Nullable String a);
    }

    // TODO: More generic type parameter tests.
}

