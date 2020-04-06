package tests.wpirunners;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import testlib.wholeprograminference.WholeProgramInferenceTestChecker;

/**
 * Runs whole-program inference and inserts annotations into source code.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/whole-program-inference/ are not
 * relevant. The meaning of this test class is to test if the generated .jaif files are similar to
 * the expected ones. The errors on .java files must be ignored.
 */
@Category(WholeProgramInferenceJaifsTest.class)
public class WholeProgramInferenceJaifsTest extends FrameworkPerDirectoryTest {
    /** @param testFiles the files containing test code, which will be type-checked */
    public WholeProgramInferenceJaifsTest(List<File> testFiles) {
        super(
                testFiles,
                WholeProgramInferenceTestChecker.class,
                "value",
                "-Anomsgtext",
                "-Ainfer=jaifs");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"whole-program-inference/non-annotated"};
    }
}
