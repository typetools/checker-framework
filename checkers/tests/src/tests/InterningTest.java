package tests;

import org.junit.*;

/**
 * JUnit tests for the Interning checker, which tests the Interned annotation.
 */
public class InterningTest extends CheckerTest {

    public InterningTest() {
        super("checkers.interning.InterningChecker", "interning", "-Anomsgtext");
    }

    /** Tests arrays. */
    @Test public void testArrays() {
        test();
    }

    /** Tests the basic semantics of Interned. */
    @Test public void testComparison() {
        test();
    }

    /** Tests Interned checks in complex conditional expressions. */
    @Test public void testComplexComparison() {
        test();
    }

    /** Tests that Interned works properly with object creation expressions. */
    @Test public void testCreation() {
        test();
    }

    /** Tests that Interned works properly with enumerated types. */
    @Test public void testEnumerations() {
        test();
    }

    /** Tests that Interned works properly with expressions. */
    @Test public void testExpressions() {
        test();
    }

    /** Tests that Interned works properly with generic types. */
    @Test public void testGenerics() {
        test();
    }

    /** Tests a standard usage scenario. */
    @Test public void testOverrideInterned() {
        test();
    }

    /** Tests a standard usage scenario. */
    @Test public void testInternMethod() {
        test();
    }

    /** Tests a standard usage scenario. */
    @Test public void testInternedClass() {
        test();
    }

    /** Tests that primitive types are implicitly treated as Interned. */
    @Test public void testPrimitives() {
        test();
    }

    /** Tests another standard usage scenario. */
    @Test public void testStaticInternMethod() {
        test();
    }

    /** Tests that the return type of String.intern is implicitly Interned. */
    @Test public void testStringIntern() {
        test();
    }

    /** Tests that SuppressWarnings works on variable declarations. */
    @Test public void testSubclass() {
        test();
    }

    /** Tests that SuppressWarnings works on variable declarations. */
    @Test public void testSuppressWarningsVar() {
        test();
    }

    /** Tests that SuppressWarnings works on class declarations. */
    @Test public void testSuppressWarningsClass() {
        test();
    }

    @Test public void testNestedGenerics() {
        test();
    }

    /** Tests that method invocation is valid **/
    @Test public void testMethodInvocation() {
        test();
    }

    @Test public void testSequenceAndIndices() {
        test();
    }

    @Test public void testHeuristics() {
        test();
    }

    // The code is illegal under javac, and the checkers should not crash.
    // Need to enhance the framework to test this; right now it's required
    // that the code be legal under javac.
    // @Test public void testDontCrash() {
    //     test();
    // }

    @Test public void testPolymorphism() {
        test();
    }
}
