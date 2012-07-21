package checkers.util.test;

import javax.tools.*;


import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

import checkers.quals.*;
import checkers.javari.quals.*;

/**
 * Abstract class for testing a checker in the Checker Framework.
 */
abstract public class CheckerTest {

    /** The fully-qualified class name of the checker to use for tests. */
    protected final String checkerName;

    /** The relative path to the directory containing test inputs. */
    protected final String checkerDir;

    /** Extra options to pass to javac when running the checker. */
    protected final String[] checkerOptions;

    /**
     * Creates a new checker test.
     *
     * @param checkerName the fully-qualified class name of the checker to use
     * @param checkerDir the path to the directory of test inputs
     * @param checkerOptions options to pass to the compiler when running tests
     */
    public CheckerTest(String checkerName, String checkerDir, String... checkerOptions) {
        this.checkerName = checkerName;
        this.checkerDir = "tests" + File.separator + checkerDir;
        this.checkerOptions = Arrays.copyOf(checkerOptions, checkerOptions.length);
    }

    /**
     * Runs a test. The method uses reflection to
     * determine the expected output and Java source files that would
     * otherwise be passed to {@link #runTest} -- if the calling method is
     * named testZZZ, this method uses an expected outfile called "ZZZ.out"
     * and a Java source file called "ZZZ.java".
     */
    protected void test(File testFile) {
        final String expectedFileName = testFile.getPath().replace(".java", ".out");
        File expectedFile = new File(expectedFileName);
        runTest(expectedFile, testFile);
    }

    protected void test() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        assert stack.length >= 3;
        String method = stack[2].getMethodName();
        if (!method.startsWith("test"))
            throw new AssertionError("caller's name is invalid");
        String[] parts = method.split("test");
        String testName = parts[parts.length - 1];
        test(new File(this.checkerDir + File.separator + testName + ".java"));
    }

    /**
     * Compiles and returns a TestRun.
     */
    protected TestRun getTest(String... files) {

        List<String> fileStrings = new LinkedList<String>();
        for (String s : files)
            fileStrings.add(checkerDir + File.separator + s);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager
            = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> tests
            = fileManager.getJavaFileObjectsFromStrings(fileStrings);

        // files need to compile cleanly without any errors
        TestRun pureCompilation = TestInput.compileAndCheck(tests, null, new String[]{});
        if (!pureCompilation.getResult()) {
            String message = "Java file is not valid Java code: " + fileStrings;
            System.err.println(message);
            for (Diagnostic<?> d : pureCompilation) {
                System.err.println(d);
            }
            throw new IllegalArgumentException(message);
        }

        return TestInput.compileAndCheck(tests, checkerName, checkerOptions);
    }

    /**
     * Compiles and returns a TestRun.
     */
    protected TestRun getTest(File... files) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager
            = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> tests
            = fileManager.getJavaFileObjects(files);

        return TestInput.compileAndCheck(tests, checkerName, checkerOptions);
    }

    /**
     * Tests that the result of compiling the javaFile matches the expectedFile.
     *
     * @param expectedFile  the expected result for compilation
     * @param javaFiles  the Java files to be compiled
     */
    protected void runTest(File expectedFile, File ... javaFiles) {
        TestRun run = getTest(javaFiles);
        if (expectedFile.exists()) {
            checkTestResult(run, expectedFile, TestUtilities.shouldSucceed(expectedFile), joinPrefixed(javaFiles, " ", this.checkerDir + File.separator));
        } else {
            List<String> expectedErrors = TestUtilities.expectedDiagnostics(this.checkerDir + File.separator, javaFiles);
            checkTestResult(run, expectedErrors, expectedErrors.isEmpty(), joinPrefixed(javaFiles, " ", this.checkerDir + File.separator));
        }
    }

    protected void runTest(List<String> expectedErrors, boolean shouldSucceed, String ... javaFiles) {
        TestRun run = getTest(javaFiles);
        checkTestResult(run, expectedErrors, shouldSucceed, joinPrefixed(javaFiles, " ", this.checkerDir + File.separator));
    }

    /**
     * Tests that the result of compiling the javaFile matches the expectedFile.
     *
     * @param expectedFileName the expected result for compilation
     * @param shouldSucceed whether the javaFile should compile successfully
     * @param javaFiles  the Java files to be compiled
     */
     protected void runTest(String expectedFileName, boolean shouldSucceed, File ... javaFiles) {
         String expectedPath = this.checkerDir + File.separator + expectedFileName;
         File expectedFile = new File(expectedPath);
         runTest(expectedFile, shouldSucceed, javaFiles);
    }

     protected void runTest(File expectedFile, boolean shouldSucceed, File ...javaFiles) {
         TestRun run = getTest(javaFiles);
         checkTestResult(run, expectedFile, shouldSucceed, join(javaFiles, " "));
     }

    protected void checkTestResult(TestRun run, File expectedFile, boolean shouldSucceed, String javaFile) {
        if (shouldSucceed)
            assertSuccess(run);
        else
            assertFailure(run);

        if ((!shouldSucceed) && !(expectedFile.exists())) {
            throw new Error("Did not find expected file " + expectedFile);
        }
        if (shouldSucceed && !(expectedFile.exists())) {
            return;
        }
        List<Diagnostic<? extends JavaFileObject>> list = run.getDiagnostics();
        assertDiagnostics(list, expectedFile, javaFile);
    }

    protected void checkTestResult(TestRun run, List<String> expectedErrors, boolean shouldSucceed, String javaFile) {
        String msg = null;
        if (shouldSucceed)
            msg = assertSuccess(run);
        else
            msg = assertFailure(run);

        List<Diagnostic<? extends JavaFileObject>> list = run.getDiagnostics();
        assertDiagnostics(msg, list, expectedErrors, javaFile);
    }

    /**
     * Asserts that the test compilation completed without failures or
     * exceptions.
     *
     * @param run the test run to check
     */
    protected String assertSuccess(/*@ReadOnly*/ TestRun run) {
        if (run.getResult()) {
            return "";
        } else {
            return "The test run was not expected to issue errors/warnings, but it did.";
        }
    }

    /**
     * Asserts that the test compilation did not complete successfully.
     *
     * @param run the test run to check
     */
    protected String assertFailure(/*@ReadOnly*/ TestRun run) {
        if (run.getResult()) {
            return "The test run was expected to issue errors/warnings, but it did not.";
        } else {
            return "";
        }
    }

    /**
     * Compares the result of the compiler against a list of errors in a file.
     * If the file is not found or cannot be read, the assertion fails.
     *
     * @param actualDiagnostics the list of diagnostics from the compiler
     * @param expectedDiagnosticFile a file containing a list of expected errors, one
     *        per line
     */
    protected void assertDiagnostics(/*@ReadOnly*/ List</*@ReadOnly*/ Diagnostic<? extends JavaFileObject>> actualDiagnostics,
            /*@ReadOnly*/ File expectedDiagnosticFile,
            String javaFile) {

        try {
            BufferedReader reader = new BufferedReader(new FileReader(expectedDiagnosticFile));
            ArrayList<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                int colonIndex = line.indexOf(':');
                if (colonIndex != -1) {
                    lines.add(line.substring(colonIndex).trim());
                } else {
                    // Either other javac output should be redirected
                    // elsewhere, so as not to confuse assertDiagnostics,
                    // or else assertDiagnostics ought to recognize other
                    // javac output.  And the file format should be defined
                    // somewhere -- what is expected to precede the first
                    // colon?  Should it always be in the first column?
                }
            }
            assertDiagnostics("", actualDiagnostics, lines, javaFile);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Compares the result of the compiler against an array of Strings.
     */
    protected void assertDiagnostics(String msg, /*@ReadOnly*/ List</*@ReadOnly*/ Diagnostic<? extends JavaFileObject>> actual_diagnostics, List</*@ReadOnly*/ String> expected_diagnostics, String filename) {
        // String cs = (checkerDir == "" ? "" : checkerDir + File.separator); // "interned"

        List<String> expectedList = new LinkedList<String>();
        for (/*@ReadOnly*/ String sd : expected_diagnostics) expectedList.add(/* cs + */ sd);

        List<String> resultsList = new LinkedList<String>();
        for (/*@ReadOnly*/ Diagnostic<? extends JavaFileObject> d : actual_diagnostics) {
            String result = d.toString().trim();
            // suppress Xlint warnings
            if (result.contains("uses unchecked or unsafe operations.") ||
                    result.contains("Recompile with -Xlint:unchecked for details.") ||
                    result.endsWith(" declares unsafe vararg methods.") ||
                    result.contains("Recompile with -Xlint:varargs for details."))
                continue;
            boolean nomsgtext = false;
            for (String opt : this.checkerOptions) {
            	if (opt.equals("-Anomsgtext")) {
            		nomsgtext = true;
            	}
            }
            if (nomsgtext) {
            	if (result.contains("\n")){
            		result = result.substring(0, result.indexOf('\n'));
            	}
            	if (result.contains(".java:")) {
            		result = result.substring(result.indexOf(".java:") + 5).trim();
            	}
            }
            resultsList.add(result);
        }
        List<String> foundList = new LinkedList<String>();
        foundList.addAll(resultsList);
        foundList.retainAll(expectedList);

        String failMessage = "";

        if ( foundList.size() != expectedList.size() ) {
            failMessage = foundList.size() + " out of "
                + expectedList.size() + " expected diagnostics "
                + (foundList.size() == 1 ? "was" : "were") +" found.\n";
        }

        boolean failed = false;

        List<String> notFoundList = new LinkedList<String>();
        notFoundList.addAll(expectedList);
        notFoundList.removeAll(resultsList);

        if (!notFoundList.isEmpty()) {
            failed = true;

            String message = notFoundList.size() == 1 ?
                "1 expected diagnostic was not found:\n" :
                notFoundList.size() + " expected diagnostics were not found:\n";
            failMessage += "\n" + message;

            for (String a : notFoundList)
                failMessage += a + "\n";

        }


        List<String> unexpectedList = new LinkedList<String>();
        unexpectedList.addAll(resultsList);
        unexpectedList.removeAll(expectedList);

        if (!unexpectedList.isEmpty()) {
            failed = true;

            String message = unexpectedList.size() == 1 ?
                "1 unexpected diagnostic was found:\n" :
                unexpectedList.size() + " unexpected diagnostics were found:\n";
            failMessage += "\n" + message;

            for (String a : unexpectedList)
                failMessage += a + "\n";
        }

        if (failed) {
            String failPrefix;

            if (msg!="") {
                failPrefix = msg + "\n";
            } else {
                failPrefix = "";
            }
            failPrefix += "While type-checking " + filename + ":\n";
            fail(failPrefix + failMessage);
        }

    }

    // Lifted from plume.UtilMDE
    /**
     * Concatenate the string representations of the objects, placing the
     * delimiter between them.
     **/
    public static String join(Object[] a, String delim) {
        if (a.length == 0) return "";
        if (a.length == 1) return String.valueOf(a[0]);
        StringBuffer sb = new StringBuffer(String.valueOf(a[0]));
        for (int i=1; i<a.length; i++)
            sb.append(delim).append(a[i]);
        return sb.toString();
    }

    /** Like join, but prefix each string by the given prefix. **/
    public static String joinPrefixed(Object[] a, String delim, String prefix) {
        if (a.length == 0) return "";
        if (a.length == 1) return prefix + String.valueOf(a[0]);
        StringBuffer sb = new StringBuffer(prefix + String.valueOf(a[0]));
        for (int i=1; i<a.length; i++)
            sb.append(delim).append(prefix).append(a[i]);
        return sb.toString();
    }

}
