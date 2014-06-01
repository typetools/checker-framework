package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportModifiersTest extends ParameterizedCheckerTest {

    public ReportModifiersTest(File testFile) {
        super(testFile,
                org.checkerframework.common.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext", "-AreportModifiers=native");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("reportmodifiers");
    }
}
