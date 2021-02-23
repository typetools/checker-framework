package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker;
import org.junit.runners.Parameterized.Parameters;

/** Create the VariableNameDefault test. */
public class VariableNameDefaultTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public VariableNameDefaultTest(List<File> testFiles) {
        super(testFiles, VariableNameDefaultChecker.class, "variablenamedefault", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"variablenamedefault"};
    }
}
