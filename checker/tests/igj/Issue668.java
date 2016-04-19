// Test case for Issue #668
// https://github.com/typetools/checker-framework/issues/668
// @skip-test

// Does not terminate when -AuseDefaultsForUncheckedCode=source is used
// but does otherwise.
public class Issue668 {
    public static boolean flag = false;
    Issue668 field;
    void foo(Issue668 param) {
        Issue668 myObject = param;
        for (Issue668 otherObject = param; myObject != null; ) {
            myObject = otherObject.field;
            if (flag) {
                otherObject = param;
            } else {
                otherObject = myObject;
            }
        }
    }
}
