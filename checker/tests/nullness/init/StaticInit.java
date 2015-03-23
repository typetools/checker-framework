// Test case for Issue 353:
// https://code.google.com/p/checker-framework/issues/detail?id=353
// @skip-test

class StaticInit {

    static String a;

    static {
        a.toString();
    }
}