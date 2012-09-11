package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class ReportModifiersTest extends ParameterizedCheckerTest {

    public ReportModifiersTest(File testFile) {
        super(testFile, checkers.util.report.ReportChecker.class.getName(), "report",
                "-Anomsgtext", "-AreportModifiers=native");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("reportmodifiers"); }
}
