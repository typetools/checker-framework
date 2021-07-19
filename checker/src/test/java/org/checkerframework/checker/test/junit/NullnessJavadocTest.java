package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.javacutil.SystemUtil;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing type-checking of code that uses Javadoc classes.
 */
public class NullnessJavadocTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public NullnessJavadocTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                toolsJarList(),
                "-Anomsgtext");
    }

    /**
     * Return a list that contains the pathname to the tools.jar file, if it exists.
     *
     * @return a list that contains the pathname to the tools.jar file, if it exists
     */
    private static List<String> toolsJarList() {
        String toolsJar = SystemUtil.getToolsJar();
        if (toolsJar == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(toolsJar);
        }
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-javadoc"};
    }
}
