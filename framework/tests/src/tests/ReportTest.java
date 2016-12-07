package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportTest extends CheckerFrameworkPerDirectoryTest {

    public ReportTest(List<File> testFiles) {
        super(
                testFiles,
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
