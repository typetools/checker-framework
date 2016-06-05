package tests;

import java.io.File;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class UnsignednessTest extends CheckerFrameworkTest {

    public UnsignednessTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.unsignedness.UnsignednessChecker.class,
                "unsignedness",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"unsignedness", "all-systems"};
    }
}
