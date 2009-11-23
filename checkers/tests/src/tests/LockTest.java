package tests;

import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LockTest extends ParameterizedCheckerTest {

    public LockTest(String testName) {
        super(testName, "checkers.lock.LockChecker", "lock", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("lock"); }
}
