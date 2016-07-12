package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingQualTest extends CheckerFrameworkTest {

    public TaintingQualTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.experimental.tainting_qual.TaintingCheckerAdapter
                        .class,
                "tainting_qual",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"tainting_qual", "all-systems"};
    }
}
