package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * Tests the constant value propagation type system.
 * 
 * @author plvines
 *
 */
public class ValueTest extends ParameterizedCheckerTest {

    public ValueTest(File testFile) {
        super(testFile, checkers.value.ValueChecker.class, "value", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("value"); }
}
