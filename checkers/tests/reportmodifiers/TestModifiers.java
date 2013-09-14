/**
 * Reported modifiers depend on the command-line invocation;
 * see checkers/tests/src/tests/ReportModifiers.java
 */
class TestModifiers {
    void test() {
        class Inner {
            //:: error: (Modifier.native)
            native void bad();
        }
    }
}