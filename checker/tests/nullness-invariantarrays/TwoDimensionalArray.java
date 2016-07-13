// @skip-test
// Test case for issue 440: https://github.com/typetools/checker-framework/issues/440

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TwoDimensionalArray {

    public static void main(String[] args) {
        assert any_null(new Object[][] {null}) == true;
    }

    public static boolean any_null(Object[] a) {
        return true;
    }
}
