// Test case for Issue 376:
// https://code.google.com/p/checker-framework/issues/detail?id=376

// @skip-test
class Test {

    interface I {}

    <Q extends Enum<Q> & I> void m(Class<Q> clazz, String name) {
        I i = Enum.valueOf(clazz, name);
    }
}