package tests;

import checkers.util.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class ReportTest extends ParameterizedCheckerTest {

    public ReportTest(File testFile) {
        super(testFile,
                checkers.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext", "-Astubs=tests/report/reporttest.astub");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("report");
    }
}
