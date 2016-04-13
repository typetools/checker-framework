package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;

public class TaintingClassicTest extends CheckerFrameworkTest {

    public TaintingClassicTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.tainting.classic.TaintingClassicChecker.class,
                "tainting_classic",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"tainting_classic", "all-systems"};
    }
}
