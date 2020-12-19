// Test case for Issue 353:
// https://github.com/typetools/checker-framework/issues/353
// @skip-test

public class StaticInit {

    static String a;

    static {
        a.toString();
    }
}
