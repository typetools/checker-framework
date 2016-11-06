package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the MethodVal Checker.
 *
 * @author smillst
 */
public class MethodValTest extends CheckerFrameworkPerDirectoryTest {

    public MethodValTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.reflection.MethodValChecker.class,
                "methodval",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"methodval"};
    }
}
