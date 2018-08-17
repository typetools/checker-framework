package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Test suite for the Subtyping Checker, using a simple {@link Encrypted} annotation. */
public class SubtypingStringPatternsPartialTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public SubtypingStringPatternsPartialTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "stringpatterns/stringpatterns-partial",
                "-Anomsgtext",
                "-Aquals=testlib.util.PatternUnknown,testlib.util.PatternAB,testlib.util.PatternBC,testlib.util.PatternAC,testlib.util.PatternBottomPartial");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stringpatterns/stringpatterns-partial", "all-systems"};
    }
}
