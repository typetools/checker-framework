// A test that throwing and catching exceptions doesn't cause false positives.

class SimpleException {
    void thrower() throws Exception {
        throw new RuntimeException("some exception");
    }

    void test() {
        try {
            thrower();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
