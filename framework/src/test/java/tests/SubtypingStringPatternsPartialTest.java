package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Test suite for the Subtyping Checker, using a simple {@link Encrypted} annotation. */
public class SubtypingStringPatternsPartialTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public SubtypingStringPatternsPartialTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "stringpatterns/stringpatterns-partial",
                "-Anomsgtext",
                "-Aquals=org.checkerframework.framework.testchecker.util.PatternUnknown,org.checkerframework.framework.testchecker.util.PatternAB,org.checkerframework.framework.testchecker.util.PatternBC,org.checkerframework.framework.testchecker.util.PatternAC,org.checkerframework.framework.testchecker.util.PatternBottomPartial");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stringpatterns/stringpatterns-partial", "all-systems"};
    }
}
