package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class UnitsTest extends CheckerFrameworkPerDirectoryTest {

    public UnitsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.units.UnitsChecker.class,
                "units",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"units", "all-systems"};
    }
}
