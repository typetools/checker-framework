package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.reflection.ReflectionTestChecker;

/** Tests the reflection resolution using a toy type system. */
public class ReflectionTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public ReflectionTest(List<File> testFiles) {
        super(testFiles, ReflectionTestChecker.class, "reflection", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"reflection"};
    }

    @Override
    public List<String> customizeOptions(List<String> previousOptions) {
        final List<String> optionsWithStub = new ArrayList<>(checkerOptions);
        optionsWithStub.add("-Astubs=" + getFullPath(testFiles.get(0), "reflection.astub"));
        optionsWithStub.add("-AresolveReflection");
        return optionsWithStub;
    }

    protected String getFullPath(final File javaFile, final String filename) {
        final String dirname = javaFile.getParentFile().getAbsolutePath();
        return dirname + File.separator + filename;
    }
}
