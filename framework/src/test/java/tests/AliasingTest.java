package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class AliasingTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public AliasingTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.aliasing.AliasingChecker.class,
                "aliasing",
                "-Anomsgtext",
                "-Astubs=tests/aliasing/stubfile.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"aliasing", "all-systems"};
    }
}
