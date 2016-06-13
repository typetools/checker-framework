import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Raw;

class FieldWithInit {
    Object f = foo();

    Object foo(@UnknownInitialization @Raw FieldWithInit this) {
        return new Object();
    }
}
