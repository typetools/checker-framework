package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.supportedquals.SupportedQualsChecker;

public class SupportedQualsTest extends CheckerFrameworkPerDirectoryTest {

    public SupportedQualsTest(List<File> testFiles) {
        super(testFiles, SupportedQualsChecker.class, "simple", "-Anomsgtext", "-AprintErrorStack");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"simple"};
    }
}
