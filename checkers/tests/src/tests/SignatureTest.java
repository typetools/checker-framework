package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SignatureTest extends ParameterizedCheckerTest {

    public SignatureTest(File testFile) {
        super(testFile, "checkers.signature.SignatureChecker", "signature", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("signature"); }
}
