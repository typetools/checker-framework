package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class SignatureTest extends CheckerFrameworkTest {

    public SignatureTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.signature.SignatureChecker.class,
                "signature",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"signature", "all-systems"};
    }
}
