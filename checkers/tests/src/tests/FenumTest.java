package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class FenumTest extends ParameterizedCheckerTest {

    public FenumTest(File testFile) {
        super(testFile, checkers.fenum.FenumChecker.class.getName(), "fenum",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("fenum", "all-systems"); }
}