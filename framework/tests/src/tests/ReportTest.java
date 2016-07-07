package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportTest extends CheckerFrameworkTest {

    public ReportTest(File testFile) {
        super(
                testFile,
                org.checkerframework.common.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext",
                "-Astubs=tests/report/reporttest.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"report"};
    }
}
