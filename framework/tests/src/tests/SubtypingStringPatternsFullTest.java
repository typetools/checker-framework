package tests;

import java.io.File;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test suite for the Subtyping Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class SubtypingStringPatternsFullTest extends CheckerFrameworkTest {

    public SubtypingStringPatternsFullTest(File testFile) {
        super(testFile,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "stringpatterns/stringpatterns-full",
                "-Anomsgtext",
                "-Aquals=tests.util.PatternUnknown,tests.util.PatternAB,tests.util.PatternBC,tests.util.PatternAC,tests.util.PatternA,tests.util.PatternB,tests.util.PatternC,tests.util.PatternBottomFull");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"stringpatterns/stringpatterns-full", "all-systems"};
    }

}
