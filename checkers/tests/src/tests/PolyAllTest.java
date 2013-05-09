package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 */
public class PolyAllTest extends ParameterizedCheckerTest {

    public PolyAllTest(File testFile) {
        super(testFile, polyall.PolyAllChecker.class.getName(), "polyall",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("polyall"); }
}