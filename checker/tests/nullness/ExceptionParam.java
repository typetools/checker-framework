import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/** Exception parameters are non-null, even if the default is nullable. */
@DefaultQualifier(org.checkerframework.checker.nullness.qual.Nullable.class)
public class ExceptionParam {
    void exc1() {
        try {
        } catch (AssertionError e) {
            @NonNull Object o = e;
        }
    }

    void exc2() {
        try {
            // :: warning: (nullness.on.exception.parameter)
        } catch (@NonNull AssertionError e) {
            @NonNull Object o = e;
        }
    }

    void exc3() {
        try {
            // :: warning: (nullness.on.exception.parameter)
        } catch (@Nullable AssertionError e) {
            @NonNull Object o = e;
        }
    }
}
