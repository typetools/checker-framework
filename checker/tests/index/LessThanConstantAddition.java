// Test case for issue #2541: https://github.com/typetools/checker-framework/issues/2541

// @skip-test until the issue is fixed

public class LessThanConstantAddition {

    public static void checkedPow(int b) {
        if (b <= 2) {
            int c = (int) b;
        }
    }
}
