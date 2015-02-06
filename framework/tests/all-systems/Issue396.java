// Test case for Issue 396:
// https://code.google.com/p/checker-framework/issues/detail?id=396
class Test {
    void b() {
        try {

        } catch (LinkageError | AssertionError e) {
            throw e;
        }
    }
}

