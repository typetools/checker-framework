package tests;

import org.junit.Test;

import checkers.basic.BasicChecker;
import checkers.util.test.*;

/**
 * Test suite for the basic checker, using a simple {@link Encrypted}
 * annotation.
 */
public class BasicSuperSubTest extends CheckerTest {

    public BasicSuperSubTest() {
        super(BasicChecker.class.getName(),
                "basic",
                "-Anomsgtext",
                "-Aquals=checkers.util.test.SubQual,checkers.util.test.SuperQual");
    }

    /** Tests basic functionality. */
    @Test public void testDependentTypes()  { test(); }
}
