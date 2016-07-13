package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class AliasingTest extends CheckerFrameworkTest {

    public AliasingTest(File testFile) {
        super(
                testFile,
                org.checkerframework.common.aliasing.AliasingChecker.class,
                "aliasing",
                "-Anomsgtext",
                "-AprintErrorStack",
                "-Astubs=tests/aliasing/stubfile.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"aliasing"};
    }
}
