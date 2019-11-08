package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.wholeprograminference.WholeProgramInferenceTestChecker;

/**
 * Tests whole-program type inference with stub files. This test is the second pass, which ensures
 * that with the stubs in place, the errors that those annotations remove are no longer issued.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/whole-program-inference/ are not
 * relevant. The meaning of this test class is to test if the generated stub files are similar to
 * the expected ones. The errors on .java files must be ignored.
 */
public class WholeProgramInferenceStubsValidationTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public WholeProgramInferenceStubsValidationTest(List<File> testFiles) {
        super(
                testFiles,
                WholeProgramInferenceTestChecker.class,
                "whole-program-inference/annotated",
                "-Anomsgtext",
                "-Astubs=build/whole-program-inference",
                /*"-AstubDebug",*/
                "-AprioritizeStubs");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"whole-program-inference/annotated/"};
    }
}
