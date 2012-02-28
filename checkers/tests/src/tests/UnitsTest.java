package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class UnitsTest extends ParameterizedCheckerTest {

    public UnitsTest(File testFile) {
        super(testFile, checkers.units.UnitsChecker.class.getName(), "units",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("units", "all-systems"); }
}