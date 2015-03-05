package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the ClassVal Checker.
 *
 * @author smillst
 *
 */
public class ClassValTest extends ParameterizedCheckerTest {

    public ClassValTest(File testFile) {
        super(testFile, org.checkerframework.common.reflection.ClassValChecker.class, "classval", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("classval");
    }
}
