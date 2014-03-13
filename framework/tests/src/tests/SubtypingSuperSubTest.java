package tests;

import checkers.util.test.CheckerTest;

import org.junit.Test;

import tests.util.Encrypted;

/**
 * Test suite for the Subtyping Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class SubtypingSuperSubTest extends CheckerTest {

    public SubtypingSuperSubTest() {
        super(checkers.subtyping.SubtypingChecker.class,
                "subtyping",
                "-Anomsgtext",
                "-Aquals=tests.util.SubQual,tests.util.SuperQual");
    }

    @Test public void dummy()  { }

    /** Tests subtyping functionality. */
    // @Test public void testDependentTypes()  { test(); }
}
