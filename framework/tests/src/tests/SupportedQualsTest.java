package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;

public class SupportedQualsTest extends CheckerFrameworkTest {

    public SupportedQualsTest(File testFile) {
        super(testFile,
                tests.supportedquals.SupportedQualsChecker.class,
                "simple",
                "-Anomsgtext",
                "-AprintErrorStack");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"simple"};
    }

}
