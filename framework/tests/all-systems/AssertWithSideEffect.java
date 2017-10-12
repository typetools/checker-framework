/* This idiom was found in Daikon and forces us to handle assignments
 * in conditional mode, such as in the condition of an assert statement. */

public class AssertWithSideEffect {
    void CheckAssert() {
        boolean assert_enabled = false;
        assert (assert_enabled = true);
    }
}
