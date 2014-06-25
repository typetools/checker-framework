package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingQualTest extends ParameterizedCheckerTest {

    public TaintingQualTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.experimental.tainting_qual.TaintingCheckerAdapter.class,
                "tainting_qual",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("tainting_qual", "all-systems");
    }
}
