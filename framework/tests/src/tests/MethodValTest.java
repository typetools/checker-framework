package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the MethodVal Checker.
 *
 * @author smillst
 *
 */
public class MethodValTest extends CheckerFrameworkTest {

    public MethodValTest(File testFile) {
        super(testFile, org.checkerframework.common.reflection.MethodValChecker.class, "methodval", "-Anomsgtext");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"methodval"};
    }
}
