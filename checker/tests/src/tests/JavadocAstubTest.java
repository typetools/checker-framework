package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the javadoc astub for the Index Checker. */
public class JavadocAstubTest extends CheckerFrameworkPerDirectoryTest {

    public JavadocAstubTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.index.IndexChecker.class,
                "index",
                "-Anomsgtext",
                "-AprintErrorStack",
                "-AstubWarnIfNotFound",
                "-Astubs=" + "lib/javadoc.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"index-javadoc-astub"};
    }
}
