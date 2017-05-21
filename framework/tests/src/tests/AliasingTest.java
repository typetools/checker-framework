package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class AliasingTest extends CheckerFrameworkPerDirectoryTest {

    public AliasingTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.aliasing.AliasingChecker.class,
                "aliasing",
                "-Anomsgtext",
                "-AprintErrorStack",
                "-Astubs=tests/aliasing/stubfile.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"aliasing", "all-systems"};
    }
}
