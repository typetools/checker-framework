package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class SignatureTest extends DefaultCheckerTest {

    public SignatureTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.signature.SignatureChecker.class,
                "signature",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"signature", "all-systems"};
    }
}
