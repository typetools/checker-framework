package tests;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.junit.Test;

import checkers.util.test.CheckerTest;

// Also see file FailedTests, that contains currently-failing tests.
/**
 * JUnit tests for the IGJ annotation checker.
 */
public class IGJTest extends CheckerTest {

    public IGJTest() {
        super(checkers.igj.IGJChecker.class.getName(), "igj", "-Anomsgtext");
    }

    void runTestWithDefault(String expectedFileName, boolean shouldSucceed, String javaFile) {
        try {
            File tempFile = File.createTempFile("Test", ".java");
            FileWriter temp = new FileWriter(tempFile);
            Scanner scanner = new Scanner(new File(checkerDir + "/" + javaFile));
            while (scanner.hasNextLine()) {
                String s = scanner.nextLine();
                s = s.replaceAll("public\\s+class", "class");
                // @Mutable is the default in most places.
                s = s.replaceAll("(?<!new )@Mutable", "");
                temp.write(s);
                temp.write('\n');
            }
            temp.flush();
            runTest(expectedFileName, shouldSucceed, tempFile);
            temp.close();
        } catch (IOException exp) {
            assertFalse("Couldn't compile file! ", true);
        }
    }

    /** Tests fields. */
    @Test public void testFields() {
        test();
    }

    @Test public void testFieldsDefault() {
        runTestWithDefault("Fields.out", false, "Fields.java");
    }

    /** Tests method invocation */
    @Test public void testMethodInvocation() {
        test();
    }

    /** Tests method invocation */
    @Test public void testMethodInvocationDefault() {
        runTestWithDefault("MethodInvocation.out", false, "MethodInvocation.java");
    }

    /** Tests Immutable Object */
    @Test public void testImmutableObject() {
        test();
    }

    /** Tests ListNode */
    @Test public void testListNode() {
        test();
    }

    /** Tests ThisReference */
    @Test public void testThisReferenceReadOnly() {
        test();
    }

    /** Tests ThisReference */
    @Test public void testThisReferenceReadOnlyDefault() {
        runTestWithDefault("ThisReferenceReadOnly.out", false, "ThisReferenceReadOnly.java");
    }

    @Test public void testThisReferenceMutable() {
        test();
    }

    /** Tests ThisReference */
    @Test public void testThisReferenceMutableDefault() {
        runTestWithDefault("ThisReferenceMutable.out", false, "ThisReferenceMutable.java");
    }

    @Test public void testThisReferenceImmutable() {
        test();
    }

    /** Tests ThisReference */
    @Test public void testThisReferenceImmutableDefault() {
        runTestWithDefault("ThisReferenceImmutable.out", false, "ThisReferenceImmutable.java");
    }

    /** Tests ForEnhanced */
    @Test public void testForEnhanced() {
        test();
    }

    @Test public void testTemplateImmutability()  {
        test();
    }

    @Test public void testTemplateImmutabilityDefault() {
        runTestWithDefault("TemplateImmutability.out", false, "TemplateImmutability.java");
    }

    @Test public void testAssignability() {
        test();
    }

    @Test public void testAssignabilityDefault() {
        runTestWithDefault("Assignability.out", false, "Assignability.java");
    }

    @Test public void testOverrideGenericMethod() {
        test();
    }

    @Test public void testArrays() {
        test();
    }

    @Test public void testPrimitives() {
        test();
    }

    @Test public void testFlow() {
        test();
    }

    @Test public void testRandomTests() {
        test();
    }

    @Test public void testMutableEnum() {
        test();
    }

    @Test public void testInnerClassesThis() {
        test();
    }

    @Test public void testInnerClassesInvok() {
        test();
    }

    @Test public void testIResolution() {
        test();
    }

    @Test public void testConstructors() {
        test();
    }

    @Test public void testGenericClass() { test(); }
    @Test public void testManifestClass()   { test(); }
    @Test public void testMutableClass()    { test(); }
    @Test public void testUnannoFieldArrayAccess() { test(); }

    // TODO: MDE will add
    // @Test public void testSubclassing() { test(); }

    // currently failing
    // @Test public void testFailedTests() { test(); }

    @Test public void testALTest1() {
        test();
    }
}
