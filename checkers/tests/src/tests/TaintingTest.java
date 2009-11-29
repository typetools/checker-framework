package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class TaintingTest extends ParameterizedCheckerTest {

    public TaintingTest(File testFile) {
        super(testFile, "checkers.tainting.TaintingChecker", "tainting", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("tainting"); }
}
