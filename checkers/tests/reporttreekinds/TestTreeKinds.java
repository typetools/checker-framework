/**
 * Reported tree kinds depend on the command-line invocation;
 * see checkers/tests/src/tests/ReportTreeKindsTest.java
 */
class TestTreeKinds {
    void test(boolean a, boolean b) {
        //:: error: (Tree.Kind.WHILE_LOOP) :: error: (Tree.Kind.CONDITIONAL_AND)
        while(a && b) {}
        if (b) {}
    }
}