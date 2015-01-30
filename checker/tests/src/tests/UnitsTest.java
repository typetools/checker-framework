package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class UnitsTest extends ParameterizedCheckerTest {

    public UnitsTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.units.UnitsChecker.class,
                "units",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("units", "all-systems");
    }
}