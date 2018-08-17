package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Test suite for the Subtyping Checker, using a simple {@link Encrypted} annotation. */
public class SubtypingStringPatternsFullTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
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
