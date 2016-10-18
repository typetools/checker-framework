package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SignednessUncheckedDefaultsTest extends CheckerFrameworkPerDirectoryTest {

    public SignednessUncheckedDefaultsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.signedness.SignednessChecker.class,
                "signedness",
                "-Anomsgtext",
                "-AprintErrorStack",
                "-AuseDefaultsForUncheckedCode=-source,bytecode");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"signedness-unchecked-defaults"};
    }
}
