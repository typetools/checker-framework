package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test suite for the Subtyping Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class SubtypingStringPatternsPartialTest extends CheckerFrameworkPerDirectoryTest {

    public SubtypingStringPatternsPartialTest(List<File> testFiles) {
        super(
                testFiles,
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
