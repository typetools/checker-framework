// Testcase for #738
// https://github.com/typetools/checker-framework/issues/738
// @skip-test
@SuppressWarnings("") // This testcase is checking for crashes.
public class Issue738 {
    public static void methodA() {
        methodB(0, new Object()); // This compiles fine.
        methodB(new int[0], new Object[0]); // This crashes.
    }

    private static <T> void methodB(T paramA, T paramB) {
        // Do nothing.
    }
}
