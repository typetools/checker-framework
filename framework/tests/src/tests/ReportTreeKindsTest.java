package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportTreeKindsTest extends CheckerFrameworkTest {

    public ReportTreeKindsTest(File testFile) {
        super(
                testFile,
                org.checkerframework.common.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext",
                "-AreportTreeKinds=WHILE_LOOP,CONDITIONAL_AND");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"reporttreekinds"};
    }
}
