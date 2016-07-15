package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingClassicTest extends CheckerFrameworkPerDirectoryTest {

    public TaintingClassicTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.tainting.classic.TaintingClassicChecker.class,
                "tainting_classic",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"tainting_classic", "all-systems"};
    }
}
