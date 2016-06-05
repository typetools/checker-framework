package tests;

import java.io.File;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class PolyAllTest extends CheckerFrameworkTest {

    public PolyAllTest(File testFile) {
        super(testFile,
                polyall.PolyAllChecker.class,
                "polyall",
                "-Anomsgtext",
                "-Astubs=tests/polyall/polyall.astub");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"polyall"};
    }
}
