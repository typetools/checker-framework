package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests signature inference with the aid of .jaif files.
 *
 * IMPORTANT: The errors captured in the tests located in tests/signature-inference/
 * are not relevant. The meaning of this test class is to test if the generated
 * .jaif files are similar to the expected ones. The errors on .java files
 * must be ignored.
 *
 * @author pbsf
 */
public class SignatureInferenceTest extends CheckerFrameworkTest {

    public SignatureInferenceTest(File testFile) {
        super(testFile, tests.signatureinference.SignatureInferenceTestChecker.class,
                "value", "-Anomsgtext");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"signature-inference/non-annotated"};
    }

    @Override
    public List<String> customizeOptions(List<String> previousOptions) {
        final List<String> options = new ArrayList<>(previousOptions);
        options.add("-AinferSignatures");
        return options;
    }

}
