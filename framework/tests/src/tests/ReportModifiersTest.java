package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportModifiersTest extends CheckerFrameworkTest {

    public ReportModifiersTest(File testFile) {
        super(
                testFile,
                org.checkerframework.common.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext",
                "-AreportModifiers=native");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"reportmodifiers"};
    }
}
