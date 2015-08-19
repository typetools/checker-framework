package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class FenumTest extends DefaultCheckerTest {

    public FenumTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.fenum.FenumChecker.class,
                "fenum",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("fenum", "all-systems");
    }
}
