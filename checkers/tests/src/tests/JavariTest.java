package tests;

import org.junit.*;

import javax.tools.*;
import java.util.*;

/**
 * JUnit tests for the Javari annotation checker.
 */
public class JavariTest extends CheckerTest {

    public JavariTest() {
        super("checkers.javari.JavariChecker", "javari", "-Anomsgtext");
    }

    /** Tests for allowed initializers */
    @Test public void testInitializers() {
        test();
    }

    /** Tests for illegal initializers */
    @Test public void testInitializers2() {
        test();
    }

    /** Tests for legal assignments */
    @Test public void testAssignments() {
        test();
    }

    /** Tests for illegal assignments */
    @Test public void testAssignments2() {
        test();
    }

    /** Tests for null mutability */
    @Test public void testNullTester() {
        test();
    }

    /** Tests for constructors */
    @Test public void testConstructors() {
        test();
    }

    /** Tests for PolyRead local variables */
    @Test public void testPolyReads() {
        test();
    }

    /** Tests for extensions */
    @Test public void testExtensions() {
        test();
    }

    /** Tests for array access */
    @Test public void testArrayTest() {
        test();
    }

    /** Tests for conditional expressions */
    @Test public void testConditionalExpressionTest() {
        test();
    }

    /** Tests for for enhanced loops */
    @Test public void testForEnhanced() {
        test();
    }

    /** Tests for readonly classes */
    @Test public void testRoClass() {
        test();
    }

    /** Test for argument mutability */
    @Test public void testArgumentMutability() {
        test();
    }

    /** Test for generics */
    @Test public void testGenerics() {
        test();
    }

    @Test public void testThisMutability() {
        test();
    }

    @Test public void testQReadOnlys() {
        test();
    }

    @Test public void testRandomTests() {
        test();
    }
}
