package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;
import tests.util.FlowTestChecker;

/**
 * Tests for the {@code -AsuggestPureMethods} command-line argument.
 *
 * @author Stefan Heule
 */
public class PuritySuggestionsTest extends CheckerFrameworkTest {

    public PuritySuggestionsTest(File testFile) {
        super(
                testFile,
                FlowTestChecker.class,
                "flow",
                "-Anomsgtext",
                "-AsuggestPureMethods",
                "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"purity-suggestions"};
    }
}
