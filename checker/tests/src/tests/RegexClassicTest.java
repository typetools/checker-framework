package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.checkerframework.framework.test.TestUtilities;

import org.junit.runners.Parameterized.Parameters;

public class RegexClassicTest extends CheckerFrameworkTest {

    public RegexClassicTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.regex.classic.RegexClassicChecker.class,
                "regex_classic",
                "-Anomsgtext");
    }

    /*
    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"regex", "regex_poly", "all-systems"};
    }
    */

    @Parameters
    public static List<File> getTestFiles() {
        return filter(TestUtilities.findNestedJavaTestFiles("regex", "regex_poly", "all-systems"));
    }

    // TODO: I want this method somewhere in ParameterizedChecker, but as
    // all these methods are static, I didn't find a fast way :-(
    protected static List<File> filter(List<File> in) {
        List<File> out = new ArrayList<File>();
        for (File file : in) {
            if (!filter(file)) {
                out.add(file);
            }
        }
        return out;
    }

    protected static boolean filter(Object o) {
        // TODO: Default qualifiers for this file seem wrong.
        return o.toString().equals("tests/regex/MatcherGroupCount.java");
    }
}
