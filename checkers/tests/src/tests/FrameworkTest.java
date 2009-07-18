package tests;

import org.junit.*;

import checkers.util.test.*;

/**
 * JUnit tests for the Checker Framework, using the {@link TestChecker}.
 */
public class FrameworkTest extends CheckerTest {

    public FrameworkTest() {
        super("checkers.util.test.TestChecker", "framework", "-Anomsgtext");
    }

    @Test public void testAnonymousClasses()            { test(); }
    @Test public void testAssignments()                 { test(); }
    @Test public void testAssignmentsGeneric()          { test(); }

    @Test public void testMethodOverrides()             { test(); }
    @Test public void testMethodOverrideBadParam()      { test(); }
    @Test public void testMethodOverrideBadReceiver()   { test(); }
    @Test public void testMethodOverrideBadReturn()     { test(); }
    @Test public void testDeepOverride()                { test(); }
    @Test public void testDeepOverrideAbstract()        { test(); }
    @Test public void testDeepOverrideInterface()       { test(); }
    @Test public void testDeepOverrideBug()             { test(); }

    @Test public void testGenericAlias()                { test(); }
    @Test public void testGenericAliasInvalid()         { test(); }
    @Test public void testGenericAliasInvalidCall()     { test(); }

    @Test public void testVarargs()                     { test(); }
    @Test public void testAnnotatedVoidMethod()         { test(); }

    // Not valid Java code.  Need to enhance framework to test this.
    // @Test public void testMissingSymbolCrash()          { test();  }
    @Test public void testMatrixBug()                   { test();  }

    @Ignore
    @Test public void testClassAnnotations()            { test(); }
    @Test public void testSupertypes()                  { test(); }

    @Test public void testSymbolError()                 { test(); }

    @Test public void testGetReceiverLoop()             { test(); }

    @Test public void testArrays()                      { test(); }
    @Test public void testTypeInference()               { test(); }

    @Test public void testPrimitiveDotClass()           { test(); }
    @Test public void testConstructors()                { test(); }
    @Test public void testOverrideCrash()               { test(); }
    @Test public void testBridgeMethods()               { test(); }
    @Test public void testRandomTests()                 { test(); }
    @Test public void testRecursiveDef()                { test(); }
    @Test public void testWildcards()                   { test(); }
    @Test public void testInnerGenerics()               { test(); }
    @Test public void testAnnotatedGenerics()           { test(); }
}
