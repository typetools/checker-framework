package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LockTest extends ParameterizedCheckerTest {

    public LockTest(File testFile) {
        super(testFile, "checkers.lock.LockChecker", "lock", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("lock"); }
}
