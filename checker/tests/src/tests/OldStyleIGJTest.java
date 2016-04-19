package tests;

import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

//TODO: The IGJ has a completely different testing mechanism from the rest of the framework
//TODO: We should probably standardize
// Also see file FailedTests, that contains currently-failing tests.
/**
 * JUnit tests for the IGJ annotation checker.
 */
public class OldStyleIGJTest {

    private final String checkerDir = "tests" + File.separator + "igj";
    private final Class<?> checker = org.checkerframework.checker.igj.IGJChecker.class;
    private final String checkerName = checker.getName();
    private final List<String> checkerOptions = Arrays.asList("-Anomsgtext");

    void runTestWithDefault(String diagnosticFileName, boolean shouldSucceed, String javaFile) {
        try {
            File tempFile = File.createTempFile("Test", ".java");
            final File originalJavaFile = new File(checkerDir + "/" + javaFile);
            FileWriter temp = new FileWriter(tempFile);
            Scanner scanner = new Scanner(originalJavaFile);
            while (scanner.hasNextLine()) {
                String s = scanner.nextLine();
                s = s.replaceAll("public\\s+class", "class");
                // @Mutable is the default in most places.
                s = s.replaceAll("(?<!new )@Mutable", "");
                temp.write(s);
                temp.write('\n');
            }
            temp.flush();
            runTest(new File(originalJavaFile.getParent(), diagnosticFileName), tempFile);
            scanner.close();
            temp.close();
            scanner.close();
        } catch (IOException exp) {
            Assert.assertFalse("Couldn't compile file! ", true);
        }
    }

    protected void runTest() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        assert stack.length >= 3;
        String method = stack[2].getMethodName();
        if (!method.startsWith("test"))
            throw new AssertionError("caller's name is invalid");
        String[] parts = method.split("test");
        String testName = parts[parts.length - 1];

        final File testFile = new File(this.checkerDir + File.separator + testName + ".java");
        runTest(testFile);
    }


    public void runTest(File testFile) {
        final File diagnostics = TestUtilities.findComparisonFile(testFile);
        runTest(diagnostics == null || !diagnostics.exists() ? null : new File(testFile.getParent(), diagnostics.getName()), testFile);
    }


    protected void runTest(File diagnosticFile, File javaFile) {
        //TODO: This is mostly the body of DefaultCheckerTest.run but with a passed in file name/exoectedDiagnostic
        //TODO: We could make it a method on TypecheckExecutor and testing would just be
        //TODO: TypecheckExecutor.test(testFile)
        boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
        TestConfigurationBuilder configBuilder =
                TestConfigurationBuilder.getDefaultConfigurationBuilder(checkerDir, javaFile, checkerName, checkerOptions, shouldEmitDebugInfo);
        if (diagnosticFile != null) {
            configBuilder.addDiagnosticFile(diagnosticFile);
        }
        TestConfiguration config = configBuilder.validateThenBuild(true);
        TypecheckResult testResult = new TypecheckExecutor().runTest(config);
        TestUtilities.assertResultsAreValid(testResult);
    }

    /** Tests fields. */
    // This test is skipped because the IGJ/OIGJ org.checkerframework.checker are not fully compatible with the latest
    // version of the Checker Framework.
    // See issue https://github.com/typetools/checker-framework/issues/199.
    @Ignore
    @Test public void testFields() {
        runTest();
    }

    // This test is skipped because the IGJ/OIGJ org.checkerframework.checker are not fully compatible with the latest
    // version of the Checker Framework.
    // See issue https://github.com/typetools/checker-framework/issues/199.
    @Ignore
    @Test public void testFieldsDefault() {
        runTestWithDefault("Fields.out", false, "Fields.java");
    }

    /** Tests method invocation */
    @Test public void testMethodInvocation() {
        runTest();
    }

    /** Tests method invocation */
    @Test public void testMethodInvocationDefault() {
        runTestWithDefault("MethodInvocation.out", false, "MethodInvocation.java");
    }

    /** Tests Immutable Object */
    @Test public void testImmutableObject() {
        runTest();
    }

    /** Tests ListNode */
    // This test is skipped because the IGJ/OIGJ org.checkerframework.checker are not fully compatible with the latest
    // version of the Checker Framework.
    // See issue https://github.com/typetools/checker-framework/issues/199.
    @Ignore
    @Test public void testListNode() {
        runTest();
    }

    /** Tests ThisReference */
    @Test public void testThisReferenceReadOnly() {
        runTest();
    }

    /** Tests ThisReference */
    @Test public void testThisReferenceReadOnlyDefault() {
        runTestWithDefault("ThisReferenceReadOnly.out", false, "ThisReferenceReadOnly.java");
    }

    @Test public void testThisReferenceMutable() {
        runTest();
    }

    /** Tests ThisReference */
    @Test public void testThisReferenceMutableDefault() {
        runTestWithDefault("ThisReferenceMutable.out", false, "ThisReferenceMutable.java");
    }

    @Test public void testThisReferenceImmutable() {
        runTest();
    }

    /** Tests ThisReference */
    @Test public void testThisReferenceImmutableDefault() {
        runTestWithDefault("ThisReferenceImmutable.out", false, "ThisReferenceImmutable.java");
    }

    /** Tests ForEnhanced */
    @Test public void testForEnhanced() {
        runTest();
    }

    @Test public void testTemplateImmutability()  {
        runTest();
    }

    @Test public void testTemplateImmutabilityDefault() {
        runTestWithDefault("TemplateImmutability.out", false, "TemplateImmutability.java");
    }


    @Test public void testAssignability() {
        runTest();
    }

    // This test is skipped because the IGJ/OIGJ org.checkerframework.checker are not fully compatible with the latest
    // version of the Checker Framework.
    // See issue https://github.com/typetools/checker-framework/issues/199.
    @Ignore
    @Test public void testAssignabilityDefault() {
        runTestWithDefault("Assignability.out", false, "Assignability.java");
    }

    @Test public void testThrowCatch() {
        runTest();
    }

    @Test public void testOverrideGenericMethod() {
        runTest();
    }

    @Test public void testArrays() {
        runTest();
    }

    @Test public void testPrimitives() {
        runTest();
    }

    @Test public void testFlow() {
        runTest();
    }

    // This test is skipped because the IGJ/OIGJ org.checkerframework.checker are not fully compatible with the latest
    // version of the Checker Framework.
    // See issue https://github.com/typetools/checker-framework/issues/199.
    @Ignore
    @Test public void testRandomTests() {
        runTest();
    }

    @Test public void testMutableEnum() {
        runTest();
    }

    @Test public void testInnerClassesThis() {
        runTest();
    }

    // This test is skipped because the IGJ/OIGJ org.checkerframework.checker are not fully compatible with the latest
    // version of the Checker Framework.
    // See issue https://github.com/typetools/checker-framework/issues/199.
    @Ignore
    @Test public void testInnerClassesInvok() {
        runTest();
    }

    @Test public void testIResolution() {
        runTest();
    }

    // This test is skipped because the IGJ/OIGJ org.checkerframework.checker are not fully compatible with the latest
    // version of the Checker Framework.
    // See issue https://github.com/typetools/checker-framework/issues/199.
    @Ignore
    @Test public void testConstructors() {
        runTest();
    }

    @Test public void testGenericClass() { runTest(); }
    @Test public void testManifestClass()   { runTest(); }
    @Test public void testMutableClass()    { runTest(); }
    @Test public void testUnannoFieldArrayAccess() { runTest(); }

    // TODO: MDE will add
    // @Test public void testSubclassing() { test(); }

    // currently failing
    // @Test public void testFailedTests() { test(); }

    @Test public void testALTest1() {
        runTest();
    }

    @Test public void testImplicitBounds() { runTest(); }
}
