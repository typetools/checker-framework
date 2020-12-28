import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class FieldWithInit {
    Object f = foo();

    Object foo(@UnknownInitialization FieldWithInit this) {
        return new Object();
    }
}
