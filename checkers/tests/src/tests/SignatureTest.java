package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class SignatureTest extends ParameterizedCheckerTest {

    public SignatureTest(File testFile) {
        super(testFile, checkers.signature.SignatureChecker.class.getName(),
                "signature", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("signature", "all-systems"); }
}
