package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import tests.util.FlowTestChecker;
import checkers.util.test.ParameterizedCheckerTest;

/**
 * Tests for the flow-sensitive part of the framework. These tests complement
 * the tests of {@link FlowTest} and have been written when the dataflow
 * analysis has been completely rewritten.
 * 
 * @author Stefan Heule
 * 
 */
public class PuritySuggestionsTest extends ParameterizedCheckerTest {

    public PuritySuggestionsTest(File testFile) {
        super(testFile, FlowTestChecker.class.getName(),
                "flow", "-Anomsgtext", "-AsuggestPureMethods");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("purity-suggestions");
    }
}
