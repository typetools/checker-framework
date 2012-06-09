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
                "-Aquals=tests.util.SubQual,tests.util.SuperQual");
    }

    @Test public void dummy()  { }

    /** Tests basic functionality. */
    // @Test public void testDependentTypes()  { test(); }
}
