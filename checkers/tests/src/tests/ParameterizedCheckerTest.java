package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(MyParameterized.class)
public abstract class ParameterizedCheckerTest extends CheckerTest {
    private final String testName;

    public ParameterizedCheckerTest(String testName,
            String checkerName, String checkerDir, String... checkerOptions) {
        super(checkerName, checkerDir, checkerOptions);
        this.testName = testName;
    }

    @Test public void run()     { test(testName); }

    protected static Collection<Object[]> testFiles(String folder) {
        File dir = new File("tests" + File.separator + folder);
        List<File> javaFiles = TestUtilities.enclosedJavaTestFiles(dir);
        Collection<Object[]> arguments = new ArrayList<Object[]>(javaFiles.size());

        for (File javaFile : javaFiles) {
            String testName = javaFile.getName().replace(".java", "");
            arguments.add(new Object[] { testName });
        }
        return arguments;
    }
}
