package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingTest extends ParameterizedCheckerTest {

    public TaintingTest(File testFile) {
        super(testFile,
                TaintingChecker.class,
                "tainting_qual_poly",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("tainting_qual_poly", "all-systems");
    }
}
