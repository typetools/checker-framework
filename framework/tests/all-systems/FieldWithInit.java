class FieldWithInit {
    @SuppressWarnings("nullness") // Don't want to depend on Nullness Checker
    Object f = foo();

    Object foo(/*@UnknownInitialization @Raw*/ FieldWithInit this) {
        return new Object();
    }
}