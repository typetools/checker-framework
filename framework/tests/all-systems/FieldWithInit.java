public class FieldWithInit {
    @SuppressWarnings("nullness") // Don't want to depend on Nullness Checker
    Object f = foo();

    Object foo(FieldWithInit this) {
        return new Object();
    }
}
