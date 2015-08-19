package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class TaintingTest extends DefaultCheckerTest {

    public TaintingTest(File testFile) {
        super(testFile,
                TaintingChecker.class,
                "tainting_qual_poly",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("tainting_qual_poly", "all-systems");
    }
}
