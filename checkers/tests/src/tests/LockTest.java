package tests;

import org.junit.*;

/**
 */
public class LockTest extends CheckerTest {

    public LockTest() {
        super("checkers.lock.LockChecker", "lock", "-Anomsgtext");
    }

    @Test public void testFields()  { test(); }
    @Test public void testMethods() { test(); }

}
