package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.common.initializedfields.InitializedFieldsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class InitializedFieldsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a InitializedFieldsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public InitializedFieldsTest(List<File> testFiles) {
        super(testFiles, InitializedFieldsChecker.class, "initialized-fields", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"initialized-fields", "all-systems"};
    }
}
