package tests;

import org.junit.*;

/**
 * JUnit tests for the Nullness checker.
 */
public class NullnessTest extends CheckerTest {

    public NullnessTest() {
        super("checkers.nullness.NullnessChecker", "nullness", "-Anomsgtext");
    }

    /** Tests for expressions-sensitive analysis. */
    @Test public void testExpressions() {
        test();
    }

    /** Tests for flow-sensitive analysis. */
    @Test public void testFlow() {
        test();
    }

    /** Tests for flow-sensitive analysis. */
    @Test public void testFlowSelf() {
        test();
    }

    /** Tests for flow-sensitive analysis. */
    @Test public void testFlowField() {
        test();
    }

    /** Tests for flow-sensitive analysis. */
    @Test public void testFlowAssignment() {
        test();
    }

    @Test public void testFlowCompound() {
        test();
    }

    /** Tests for flow-sensitive analysis. */
    @Test public void testMyException() {
        test();
    }

    /** Tests that crashes do not occur for varargs methods. */
    @Test public void testVarargs() {
        test();
    }

    /** Tests the @DefaultQualifier annotation. */
    @Test public void testDefaultAnnotation() {
        test();
    }

    @Test public void testArrayRefs() {
        test();
    }

    @Test public void testArrayArgs() {
        test();
    }

    @Test public void testDefaultFlow() {
        test();
    }

    @Test public void testGenericArgs() {
        test();
    }

    @Test public void testNullableGeneric() {
        test();
    }

    @Test public void testMarino() {
        test();
    }

    @Test public void testNonNullMapValue() {
        test();
    }

    @Test public void testPackageDecl() {
        test();
    }

    @Test public void testJavaCopExplosion() {
        test();
    }

    @Test public void testJavaCopFlow() {
        test();
    }

    @Test public void testJavaCopRandomTests() {
        test();
    }

    @Test public void testRawTypes() {
        test();
    }

    @Test public void testThisTest() {
        test();
    }

    @Test public void testDotClass() {
        test();
    }

    @Test public void testDefaultInterface() {
        test();
    }

    @Test public void testMultiAnnotations() {
        test();
    }

    @Test public void testPolymorphism() {
        test();
    }

    @Test public void testExceptions() {
        test();
    }

    @Test public void testSynchronization() {
        test();
    }

    @Test public void testBoxing() {
        test();
    }

    @Test public void testEnums() {
        test();
    }

    @Test public void testVoidUse() {
        test();
    }

    @Test public void testCasts() {
        test();
    }

    @Test public void testAsserts() {
        test();
    }

    @Test public void testLogicOperations() {
        test();
    }

    @Test public void testLoopFlow() {
        test();
    }

    @Test public void testAnnotatedGenerics() {
        test();
    }

    @Test public void testNullnessAssertion() {
        test();
    }

    @Test public void testToArray() { test(); }

    @Test public void testDependentNull() { test(); }

    @Test public void testFlowConstructor() { test(); }

    @Test public void testLazyInitialization() { test(); }
}
