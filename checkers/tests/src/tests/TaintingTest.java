package tests;

import org.junit.Test;

public class TaintingTest extends CheckerTest {

    public TaintingTest() {
        super("checkers.tainting.TaintingChecker", "tainting", "-Anomsgtext");
    }

    @Test
    public void testSimple()                 { test(); }
}
