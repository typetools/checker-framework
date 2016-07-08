package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class SignednessTest extends CheckerFrameworkTest {

    public SignednessTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.signedness.SignednessChecker.class,
                "signedness",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"signedness", "all-systems"};
    }
}
