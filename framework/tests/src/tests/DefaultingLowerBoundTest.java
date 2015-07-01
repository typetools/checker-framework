package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * Created by jburke on 9/29/14.
 */
public class DefaultingLowerBoundTest extends ParameterizedCheckerTest {

    public DefaultingLowerBoundTest(File testFile) {
        super(testFile,
              tests.defaulting.DefaultingLowerBoundChecker.class,
              "defaulting",
              "-Anomsgtext"
        );
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("defaulting/lowerbound");
    }
}
