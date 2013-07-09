package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import tests.util.FlowTestChecker;
import checkers.util.test.ParameterizedCheckerTest;

/**
 * Tests for the {@code -AsuggestPureMethods} command-line argument.
 * 
 * @author Stefan Heule
 */
public class PuritySuggestionsTest extends ParameterizedCheckerTest {

    public PuritySuggestionsTest(File testFile) {
        super(testFile, FlowTestChecker.class.getName(),
              "flow", "-Anomsgtext", "-AsuggestPureMethods", "-AenablePurity");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("purity-suggestions");
    }
}
