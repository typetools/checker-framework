package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Test suite for the Subtyping Checker, using a simple {@link Encrypted} annotation. */
public class SubtypingStringPatternsFullTest extends CheckerFrameworkPerDirectoryTest {

    public SubtypingStringPatternsFullTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "stringpatterns/stringpatterns-full",
                "-Anomsgtext",
                "-Aquals=testlib.util.PatternUnknown,testlib.util.PatternAB,testlib.util.PatternBC,testlib.util.PatternAC,testlib.util.PatternA,testlib.util.PatternB,testlib.util.PatternC,testlib.util.PatternBottomFull");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stringpatterns/stringpatterns-full", "all-systems"};
    }
}
