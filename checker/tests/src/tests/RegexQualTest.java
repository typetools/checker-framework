package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class RegexQualTest extends CheckerFrameworkPerFileTest {

    public RegexQualTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.experimental.regex_qual.RegexCheckerAdapter.class,
                "regex_qual",
                "-Anomsgtext");
    }

    /*
    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"regex", "all-systems"};
    }
    */

    @Parameters
    public static List<File> getTestFiles() {
        return filter(TestUtilities.findNestedJavaTestFiles("regex", "all-systems"));
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
