package tests;

import org.junit.Test;

import tests.util.Encrypted;
import checkers.subtyping.SubtypingChecker;
import checkers.util.test.CheckerTest;

/**
 * Test suite for the Basic Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class BasicSuperSubTest extends CheckerTest {

    public BasicSuperSubTest() {
        super(SubtypingChecker.class.getName(),
                "basic",
                "-Anomsgtext",
                "-Aquals=tests.util.SubQual,tests.util.SuperQual");
    }

    @Test public void dummy()  { }

    /** Tests basic functionality. */
    // @Test public void testDependentTypes()  { test(); }
}
