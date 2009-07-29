package tests;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * JUnit tests for the IGJ annotation checker.
 */
public class IGJTest extends CheckerTest {

    public IGJTest() {
        super("checkers.igj.IGJChecker", "igj", "-Anomsgtext");
    }

    void runTestWithDefault(String expected, boolean shouldSucceed, String javaFile) {
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
            runTest(expected, shouldSucceed, tempFile);
            temp.close();
        } catch (IOException exp) {
            assertFalse("Couldn't compile file! ", true);
        }
    }

    /** Tests fields. */
    @Test public void testFields() {
        runTest("Fields.out", false, "Fields.java");
    }

    @Test public void testFieldsDefault() {
        runTestWithDefault("Fields.out", false, "Fields.java");
    }

    /** Tests method invocation */
    @Test public void testMethodInvocation() {
        runTest("MethodInvocation.out", false, "MethodInvocation.java");
    }

    /** Tests method invocation */
    @Test public void testMethodInvocationDefault() {
        runTestWithDefault("MethodInvocation.out", false, "MethodInvocation.java");
    }

    /** Tests Immutable Object */
    @Test public void testImmutableObject() {
        runTest("ImmutableObject.out", false, "ImmutableObject.java");
    }

    /** Tests ListNode */
    @Ignore
    @Test public void testListNode() {
        runTest("ListNode.out", false, "ListNode.java");
    }

    /** Tests ThisReference */
    @Ignore @Test public void testThisReference() {
        runTest("ThisReference.out", false, "ThisReference.java");
    }

    /** Tests ThisReference */
    @Ignore @Test public void testThisReferenceDefault() {
        runTestWithDefault("ThisReference.out", false, "ThisReference.java");
    }

    /** Tests ForEnhanced */
    @Test public void testForEnhanced() {
        runTest("ForEnhanced.out", false, "ForEnhanced.java");
    }

    @Ignore
    @Test public void testForEnhancedDefault() {
        runTestWithDefault("ForEnhanced.out", false, "ForEnhanced.java");
    }

    @Ignore
    @Test public void testTemplateImmutability()  {
        runTest("TemplateImmutability.out", false, "TemplateImmutability.java");
    }

    @Ignore
    @Test public void testTemplateImmutabilityDefault() {
        runTestWithDefault("TemplateImmutability.out", false, "TemplateImmutability.java");
    }

    @Test public void testAssignability() {
        runTest("Assignability.out", false, "Assignability.java");
    }

    @Test public void testAssignabilityDefault() {
        runTestWithDefault("Assignability.out", false, "Assignability.java");
    }

    @Test public void testOverrideGenericMethod() {
        runTest("OverrideGenericMethod.out", true, "OverrideGenericMethod.java");
    }

    @Test public void testArrays() {
        runTest("Arrays.out", false, "Arrays.java");
    }

    @Test public void testPrimitives() {
        runTest("Primitives.out", true, "Primitives.java");
    }

    @Test public void testFlow() {
        runTest("Flow.out", false, "Flow.java");
    }

    @Test public void testRandomTests() {
        runTest("RandomTests.out", true, "RandomTests.java");
    }

    @Test public void testMutableEnum() {
        runTest("Mutable.out", true, "MutableEnum.java");
    }

    @Test public void testInnerClassesThis() {
        runTest("InnerClassesThis.out", false, "InnerClassesThis.java");
    }

    @Test public void testInnerClassesInvok() {
        runTest("InnerClassesInvok.out", false, "InnerClassesInvok.java");
    }

    @Test public void testIResolution() {
        runTest("IResolution.out", true, "IResolution.java");
    }
}
