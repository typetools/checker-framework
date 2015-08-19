package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class ReportTest extends DefaultCheckerTest {

    public ReportTest(File testFile) {
        super(testFile,
                org.checkerframework.common.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext", "-Astubs=tests/report/reporttest.astub");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("report");
    }
}
