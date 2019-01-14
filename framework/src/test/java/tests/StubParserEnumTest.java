package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class StubParserEnumTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public StubParserEnumTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.util.report.ReportChecker.class,
                "stubparser",
                "-Anomsgtext",
                "-Astubs=tests/stubparser/enumconstants.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubparser"};
    }
}
