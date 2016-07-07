package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test suite for the Subtyping Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class SubtypingStringPatternsPartialTest extends CheckerFrameworkTest {

    public SubtypingStringPatternsPartialTest(File testFile) {
        super(
                testFile,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "stringpatterns/stringpatterns-partial",
                "-Anomsgtext",
                "-Aquals=tests.util.PatternUnknown,tests.util.PatternAB,tests.util.PatternBC,tests.util.PatternAC,tests.util.PatternBottomPartial");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stringpatterns/stringpatterns-partial", "all-systems"};
    }
}
