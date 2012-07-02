package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class FenumSwingTest extends ParameterizedCheckerTest {

    public FenumSwingTest(File testFile) {
        super(
                testFile,
                checkers.fenum.FenumChecker.class.getName(),
                "fenum",
                "-Anomsgtext",
                "-Alint=flowinference",
                "-Aquals=checkers.fenum.quals.SwingVerticalOrientation,checkers.fenum.quals.SwingHorizontalOrientation,checkers.fenum.quals.SwingBoxOrientation,checkers.fenum.quals.SwingCompassDirection,checkers.fenum.quals.SwingElementOrientation,checkers.fenum.quals.SwingTextOrientation");
        // TODO: check all qualifiers
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("fenumswing", "all-systems"); }
}