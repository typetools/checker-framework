package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class ReportModifiersTest extends DefaultCheckerTest {

    public ReportModifiersTest(File testFile) {
        super(testFile,
                org.checkerframework.common.util.report.ReportChecker.class,
                "report",
                "-Anomsgtext", "-AreportModifiers=native");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"reportmodifiers"};
    }
}
