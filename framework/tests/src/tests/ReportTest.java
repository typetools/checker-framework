package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportTest extends ParameterizedCheckerTest {

    public ReportTest(File testFile) {
        super(testFile,
                org.checkerframework.common.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext", "-Astubs=tests/report/reporttest.astub");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("report");
    }
}
