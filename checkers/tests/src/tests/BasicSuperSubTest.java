package tests;

import checkers.util.test.CheckerTest;

import org.junit.Test;

import tests.util.Encrypted;

/**
 * Test suite for the Basic Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class BasicSuperSubTest extends CheckerTest {

    public BasicSuperSubTest() {
        super(checkers.subtyping.SubtypingChecker.class,
                "basic",
                "-Anomsgtext",
                "-Aquals=tests.util.SubQual,tests.util.SuperQual");
    }

    @Test public void dummy()  { }

    /** Tests basic functionality. */
    // @Test public void testDependentTypes()  { test(); }
}
