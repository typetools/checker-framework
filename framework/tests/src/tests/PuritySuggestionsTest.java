package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

import tests.util.FlowTestChecker;

/**
 * Tests for the {@code -AsuggestPureMethods} command-line argument.
 *
 * @author Stefan Heule
 */
public class PuritySuggestionsTest extends DefaultCheckerTest {

    public PuritySuggestionsTest(File testFile) {
        super(testFile,
                FlowTestChecker.class,
                "flow",
                "-Anomsgtext", "-AsuggestPureMethods", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"purity-suggestions"};
    }
}
