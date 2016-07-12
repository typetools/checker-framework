package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class UnitsTest extends CheckerFrameworkTest {

    public UnitsTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.units.UnitsChecker.class,
                "units",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"units", "all-systems"};
    }
}
