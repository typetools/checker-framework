package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SupportedQualsTest extends CheckerFrameworkPerDirectoryTest {

    public SupportedQualsTest(List<File> testFiles) {
        super(
                testFiles,
                tests.supportedquals.SupportedQualsChecker.class,
                "simple",
                "-Anomsgtext",
                "-AprintErrorStack");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"simple"};
    }
}
