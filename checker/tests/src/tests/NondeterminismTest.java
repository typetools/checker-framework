package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class NondeterminismTest extends CheckerFrameworkPerDirectoryTest {

    public NondeterminismTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nondeterminism.NondeterminismChecker.class,
                "nondeterminism",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nondeterminism", "all-systems"};
    }
}
