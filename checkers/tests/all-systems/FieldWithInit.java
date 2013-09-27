import checkers.initialization.quals.UnknownInitialization;
import checkers.nullness.quals.Raw;

class FieldWithInit {
    Object f = foo();

    Object foo(@UnknownInitialization @Raw FieldWithInit this) {
        return new Object();
    }
}