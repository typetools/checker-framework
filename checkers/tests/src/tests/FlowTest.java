package tests;

import org.junit.*;

/**
 */
public class FlowTest extends CheckerTest {

    public FlowTest() {
        super("checkers.util.test.FlowTestChecker", "flow");
    }

    @Test public void testBasic() { test(); }
    @Test public void testFields() { test(); }
    @Test public void testMoreFields() { test(); }
}
