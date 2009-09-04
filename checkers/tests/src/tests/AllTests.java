// This file appears to be IGNORED.
// Instead, see target all-tests in file ../../../build.xml


package tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;

import junit.framework.*;

/**
 * JUnit 3 Test suite that dynamically
 */
public class AllTests {

    public static final String[] CHECKERS = {
        "BasicEncrypted",
        "BasicSuperSub",
        "Flow",
        "Framework",
        "IGJ",
        "Interning",
        "Javari",
        "Nullness"
    };

    public static Test suite() {
        TestSuite suite = new TestSuite("All Checkers");
        //$JUnit-BEGIN$
//        for (String checker : CHECKERS)
//            suite.addTest(createCheckerTestSuite(checker));
//
//        File thisDirectory = new File("tests");
//        List<File> allJavaFiles = TestUtilities.deeplyEnclosedJavaTestFiles(thisDirectory);
//
//        for (String checker : CHECKERS)
//            suite.addTest(checkForExceptions(checker, allJavaFiles));

        //$JUnit-END$
        return suite;
    }

    private static TestSuite checkForExceptions(final String checker, List<File> allFiles) {
        final CheckerTest checkerTest = getCheckerTestClass(checker);
        Class<? extends CheckerTest> clz = checkerTest.getClass();
        TestSuite testSuite = new TestSuite("all." + checker);

        int count = 0;

        for (final File javaFile : allFiles) {
            final String testCaseName = javaFile.getName().replace(".java", "");
            final String testName = (++count) + ".all." + checker + "." + testCaseName;
            testSuite.addTest(new TestCase(testName) {
                public void runTest() {
                    // Get any exceptions
                    PrintStream old = System.err;

                    ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                    System.setErr(new PrintStream(errorStream));
                    try {
                        checkerTest.getTest(javaFile);
                    } finally {
                        System.setErr(old);
                    }

                    if (errorStream.size() == 0)
                        assertTrue(true);
                    else
                        assertEquals(errorStream.toString(), "");
                }
            });
        }
        return testSuite;
    }

    private static TestSuite createCheckerTestSuite(final String checker) {
        final CheckerTest checkerTest = getCheckerTestClass(checker);

        TestSuite testSuite = new TestSuite(checker);

        File checkerDir = new File(checkerTest.checkerDir);
        List<File> javaFiles = TestUtilities.enclosedJavaTestFiles(checkerDir);

        for (final File javaFile : javaFiles) {
            final String testCaseName = javaFile.getName().replace(".java", "");
            testSuite.addTest(new TestCase(checker + "." + testCaseName) {
                public void runTest() {
                    String expectedOutput = javaFile.getAbsolutePath().replace(".java", ".out");
                    File expectedFile = new File(expectedOutput);
                    boolean shouldSucceed = TestUtilities.shouldSucceed(expectedFile);
                    checkerTest.runTest(expectedFile, shouldSucceed, javaFile);
                }
            });
        }
        return testSuite;
    }

    private static CheckerTest getCheckerTestClass(String checker) {
        String thisPackage = AllTests.class.getPackage().getName();
        String className = thisPackage + "." + checker + "Test";
        try {
            Class<?> testClass = Class.forName(className);
            Object o = testClass.newInstance();
            return (CheckerTest)o;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private String expectedFromJavaFile(String javaFile) {
        return javaFile.replace(".java", ".out");
    }


}
