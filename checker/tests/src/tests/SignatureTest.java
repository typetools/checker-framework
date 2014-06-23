package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class SignatureTest extends ParameterizedCheckerTest {

    public SignatureTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.signature.SignatureChecker.class,
                "signature",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("signature", "all-systems");
    }
}
