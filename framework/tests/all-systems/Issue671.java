// Test case for Issue #671
// https://github.com/typetools/checker-framework/issues/671
class Issue671 {

    @SuppressWarnings("determinism:invalid.type.on.conditional")
    void foo() {
        byte var = 0;
        boolean f = (var == (method() ? 2 : 0));
    }

    boolean method() {
        return false;
    }
}
