package tests;

import checkers.util.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the flow-sensitive part of the framework. These tests complement
 * the tests of {@link FlowTest} and have been written when the dataflow
 * analysis has been completely rewritten.
 *
 * @author Stefan Heule
 *
 */
public class Flow2Test extends ParameterizedCheckerTest {

    public Flow2Test(File testFile) {
        super(testFile,
                tests.util.FlowTestChecker.class,
                "flow",
                "-Anomsgtext", "-AenablePurity");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("flow2");
    }
}
