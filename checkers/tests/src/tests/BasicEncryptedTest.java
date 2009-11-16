package tests;

import org.junit.Test;

import checkers.basic.BasicChecker;
import checkers.util.test.Encrypted;

/**
 * Test suite for the basic checker, using a simple {@link Encrypted}
 * annotation.
 */
public class BasicEncryptedTest extends CheckerTest {

    public BasicEncryptedTest() {
        super(BasicChecker.class.getName(),
                "basic",
                "-Anomsgtext",
                "-Aquals=checkers.util.test.Encrypted,checkers.util.test.PolyEncrypted");
    }

    /** Tests basic functionality. */
    @Test public void testSimple()          { test(); }
    @Test public void testPoly()            { test(); }
}
