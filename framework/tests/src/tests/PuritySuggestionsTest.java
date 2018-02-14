package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.util.FlowTestChecker;

/** Tests for the {@code -AsuggestPureMethods} command-line argument. */
public class PuritySuggestionsTest extends CheckerFrameworkPerDirectoryTest {

    public PuritySuggestionsTest(List<File> testFiles) {
        super(
                testFiles,
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
