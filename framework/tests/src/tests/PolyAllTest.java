package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** */
public class PolyAllTest extends CheckerFrameworkPerDirectoryTest {

    public PolyAllTest(List<File> testFiles) {
        super(
                testFiles,
                polyall.PolyAllChecker.class,
                "polyall",
                "-Anomsgtext",
                "-Astubs=tests/polyall/polyall.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"polyall"};
    }
}
