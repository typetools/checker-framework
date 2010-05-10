package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Interning checker, which tests the Interned annotation.
 */
public class OIGJTest extends ParameterizedCheckerTest {

    public OIGJTest(File testFile) {
        super(testFile, "checkers.oigj.OIGJChecker", "oigj", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("oigj"); }
}
