// Test case for Issue 396:
// https://github.com/typetools/checker-framework/issues/396
class Issue396 {
    void b() {
        try {

        } catch (LinkageError | AssertionError e) {
            throw e;
        }
    }
}
