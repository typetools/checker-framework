// TODO: "this" should be raw in field initializers.
class FieldWithSomeInit {
    // The call of foo() will result in a NPE, because g is
    // not initialized yet. Making foo raw will forbid
    // dereferencing field g.
    Object f = foo();
    Object g = new Object();

    Object foo() {
        return g.toString();
    }
}