package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class ReportTreeKindsTest extends ParameterizedCheckerTest {

    public ReportTreeKindsTest(File testFile) {
        super(testFile, checkers.util.report.ReportChecker.class.getName(), "report",
                "-Anomsgtext", "-AreportTreeKinds=WHILE_LOOP,CONDITIONAL_AND");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("reporttreekinds"); }
}
