package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests the ClassVal Checker. */
public class ClassValTest extends FrameworkPerDirectoryTest {

    public ClassValTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.reflection.ClassValChecker.class,
                "classval",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"classval"};
    }
}
