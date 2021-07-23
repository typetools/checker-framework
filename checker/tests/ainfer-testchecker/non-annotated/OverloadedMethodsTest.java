// This test ensures that overloaded methods with different return types aren't confused.

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

public class OverloadedMethodsTest {

    String f;

    String m1() {
        return this.f;
    }

    String m1(String x) {
        return getSibling1();
    }

    @Sibling1 String getSibling1() {
        return null;
    }
}
