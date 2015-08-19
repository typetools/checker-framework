package tests;

import java.io.File;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the reflection resolution using a toy type system.
 *
 * @author rjust, smillst
 *
 */
public class ReflectionTest extends DefaultCheckerTest {

    public ReflectionTest(File testFile) {
        super(testFile, tests.reflection.ReflectionTestChecker.class, "reflection", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("reflection");
    }

    @Override
    public List<String> customizeOptions(List<String> previousOptions) {
        final List<String> optionsWithStub = new ArrayList<>(checkerOptions);
        optionsWithStub.add("-Astubs=" + getFullPath(testFile, "reflection.astub"));
        optionsWithStub.add("-AresolveReflection");
        return optionsWithStub;
    }

    protected String getFullPath(final File javaFile, final String filename) {
        final String dirname = javaFile.getParentFile().getAbsolutePath();
        return dirname + File.separator + filename;
    }
}
