class FieldWithInit {
    Object f = foo();

    Object foo() {
        return new Object();
    }
}