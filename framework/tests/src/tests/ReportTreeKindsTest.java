package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportTreeKindsTest extends ParameterizedCheckerTest {

    public ReportTreeKindsTest(File testFile) {
        super(testFile,
                org.checkerframework.common.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext", "-AreportTreeKinds=WHILE_LOOP,CONDITIONAL_AND");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("reporttreekinds");
    }
}
