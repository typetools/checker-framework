package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LockTest extends ParameterizedCheckerTest {

    public LockTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.lock.LockChecker.class,
                "lock",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("lock", "all-systems");
    }
}
