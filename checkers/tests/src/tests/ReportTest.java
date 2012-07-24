package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class ReportTest extends ParameterizedCheckerTest {

    public ReportTest(File testFile) {
        super(testFile, checkers.util.report.ReportChecker.class.getName(), "report",
                "-Anomsgtext", "-Astubs=tests/report/reporttest.astub");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("report"); }
}
