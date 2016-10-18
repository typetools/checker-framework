package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SignednessTest extends CheckerFrameworkPerDirectoryTest {

    public SignednessTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.signedness.SignednessChecker.class,
                "signedness",
                "-Anomsgtext",
                "-AprintErrorStack");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"signedness", "all-systems"};
    }
}
