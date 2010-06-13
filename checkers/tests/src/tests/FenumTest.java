package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class FenumTest extends ParameterizedCheckerTest {

    public FenumTest(File testFile) {
        super(testFile, "checkers.fenum.FenumChecker", "fenum", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("fenum"); }
}