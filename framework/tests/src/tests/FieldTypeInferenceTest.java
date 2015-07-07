package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the private field inference using a toy type system.
 *
 * @author pbsf
 */
public class FieldTypeInferenceTest extends ParameterizedCheckerTest {

    public FieldTypeInferenceTest(File testFile) {
        super(testFile, tests.fieldtypeinference.FieldTypeInferenceTestChecker.class, "field-type-inference", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("field-type-inference");
    }

    @Override
    protected void test(File testFile) {
        final List<String> optionsWithStub = new ArrayList<>(checkerOptions);
        test(checkerName, optionsWithStub, testFile);
    }

    protected String getFullPath(final File javaFile, final String filename) {
        final String dirname = javaFile.getParentFile().getAbsolutePath();
        return dirname + System.getProperty("file.separator") + filename;
    }
}
