package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the MethodVal Checker.
 *
 * @author smillst
 *
 */
public class MethodValTest extends ParameterizedCheckerTest {

    public MethodValTest(File testFile) {
        super(testFile, org.checkerframework.common.reflection.MethodValChecker.class, "methodval", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("methodval");
    }
}
