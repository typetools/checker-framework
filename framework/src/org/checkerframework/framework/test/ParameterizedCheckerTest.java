package org.checkerframework.framework.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.AbstractProcessor;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CheckerParameterized.class)
public abstract class ParameterizedCheckerTest extends CheckerTest {
    private final File testFile;

    public ParameterizedCheckerTest(File testFile,
            Class<? extends AbstractProcessor> checker, String checkerDir, String... checkerOptions) {
        super(checker, checkerDir, checkerOptions);
        this.testFile = testFile;
    }

    @Test
    public void run() {
        test(testFile);
    }

    protected static Collection<Object[]> testFiles(String... folders) {
        Collection<Object[]> arguments = new ArrayList<Object[]>();
        for (String folder : folders) {
            File dir = new File("tests" + File.separator + folder);
            List<File> javaFiles = TestUtilities.deeplyEnclosedJavaTestFiles(dir);

            for (File javaFile : javaFiles) {
                arguments.add(new Object[] { javaFile });
            }
        }
        return arguments;
    }
}
