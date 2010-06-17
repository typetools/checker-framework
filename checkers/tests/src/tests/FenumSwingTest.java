package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class FenumSwingTest extends ParameterizedCheckerTest {

    public FenumSwingTest(File testFile) {
        super(testFile, "checkers.fenum.FenumChecker", "fenum", "-Anomsgtext", "-Aquals=checkers.fenum.quals.SwingBoxOrientation");
        // TODO: check all qualifiers
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("fenumswing"); }
}