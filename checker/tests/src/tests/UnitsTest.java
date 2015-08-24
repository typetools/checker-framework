package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class UnitsTest extends DefaultCheckerTest {

    public UnitsTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.units.UnitsChecker.class,
                "units",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"units", "all-systems"};
    }
}