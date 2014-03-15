package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the constant value propagation type system.
 * 
 * @author plvines
 *
 */
public class ValueTest extends ParameterizedCheckerTest {

    public ValueTest(File testFile) {
        super(testFile, org.checkerframework.common.value.ValueChecker.class, "value", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("value"); }
}
