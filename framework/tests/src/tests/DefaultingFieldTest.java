package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;

/**
 * Created by jburke on 9/29/14.
 */
public class DefaultingFieldTest extends CheckerFrameworkTest {

    public DefaultingFieldTest(File testFile) {
        super(testFile,
                tests.defaulting.DefaultingFieldChecker.class,
                "defaulting",
                "-Anomsgtext"
        );
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"defaulting/field"};
    }
}
