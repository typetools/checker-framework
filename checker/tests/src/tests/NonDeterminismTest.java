package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class NonDeterminismTest extends CheckerFrameworkPerDirectoryTest {

    public NonDeterminismTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nondeterminism.NonDeterminismChecker.class,
                "nondeterminism",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nondeterminism", "all-systems"};
    }
}
