package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LockTest extends DefaultCheckerTest {

    public LockTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.lock.LockChecker.class,
                "lock",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("lock", "all-systems");
    }
}
