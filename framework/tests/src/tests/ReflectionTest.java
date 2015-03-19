package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the reflection resolution using a toy type system.
 *
 * @author rjust, smillst
 *
 */
public class ReflectionTest extends ParameterizedCheckerTest {

    public ReflectionTest(File testFile) {
        super(testFile, tests.reflection.ReflectionTestChecker.class, "reflection", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("reflection");
    }

    @Override
    protected void test(File testFile) {
        final List<String> optionsWithStub = new ArrayList<>(checkerOptions);
        optionsWithStub.add("-Astubs=" + getFullPath(testFile, "reflection.astub"));
        optionsWithStub.add("-AresolveReflection");
        test(checkerName, optionsWithStub, testFile);
    }

    protected String getFullPath(final File javaFile, final String filename) {
        final String dirname = javaFile.getParentFile().getAbsolutePath();
        return dirname + System.getProperty("file.separator") + filename;
    }
}
