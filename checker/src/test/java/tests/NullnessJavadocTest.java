package tests;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
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
                toolsJar(),
                "-Anomsgtext");
    }

    /**
     * Return a list that contains the pathname to the tools.jar file, if it exists.
     *
     * @returns a list that contains the pathname to the tools.jar file, or an empty list
     */
    private static List<String> toolsJar() {
        if (!System.getProperty("java.version").startsWith("1.8")) {
            return Collections.emptyList();
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            String javaHomeProperty = System.getProperty("java.home");
            if (javaHomeProperty.endsWith(File.separator + "jre")) {
                javaHome = javaHomeProperty.substring(javaHomeProperty.length() - 4);
            } else {
                // Could also determine the location of javac on the path...
                throw new Error("Can't infer Java home");
            }
        }
        String toolsJar = javaHome + File.separator + "lib" + File.separator + "tools.jar";
        return Collections.singletonList(toolsJar);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-javadoc"};
    }
}
