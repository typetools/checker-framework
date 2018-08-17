package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** */
public class PolyAllTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
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
