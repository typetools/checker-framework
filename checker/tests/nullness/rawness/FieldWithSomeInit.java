class FieldWithSomeInit {
    // :: error: (method.invocation.invalid)
    Object f = foo();
    Object g = new Object();

    Object foo() {
        return g.toString();
    }
}
