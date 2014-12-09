package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingQualPolyTest extends ParameterizedCheckerTest {

    public TaintingQualPolyTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.experimental.tainting_qual_poly.TaintingCheckerAdapter.class,
                "tainting_qual_poly",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("tainting_qual_poly", "all-systems");
    }
}
