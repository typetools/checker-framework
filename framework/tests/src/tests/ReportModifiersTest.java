package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportModifiersTest extends CheckerFrameworkPerDirectoryTest {

    public ReportModifiersTest(List<File> testFiles) {
        super(
                testFiles,
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
