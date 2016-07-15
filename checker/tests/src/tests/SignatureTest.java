package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SignatureTest extends CheckerFrameworkPerDirectoryTest {

    public SignatureTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.signature.SignatureChecker.class,
                "signature",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"signature", "all-systems"};
    }
}
