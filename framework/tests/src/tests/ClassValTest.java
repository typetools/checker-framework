package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the ClassVal Checker.
 *
 * @author smillst
 *
 */
public class ClassValTest extends CheckerFrameworkTest {

    public ClassValTest(File testFile) {
        super(
                testFile,
                org.checkerframework.common.reflection.ClassValChecker.class,
                "classval",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"classval"};
    }
}
