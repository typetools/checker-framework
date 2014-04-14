package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingTest extends ParameterizedCheckerTest {

    public TaintingTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.tainting.TaintingChecker.class,
                "tainting",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("tainting", "all-systems");
    }
}
