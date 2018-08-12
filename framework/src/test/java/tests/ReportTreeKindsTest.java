package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class ReportTreeKindsTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public ReportTreeKindsTest(List<File> testFiles) {
        super(
                testFiles,
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
