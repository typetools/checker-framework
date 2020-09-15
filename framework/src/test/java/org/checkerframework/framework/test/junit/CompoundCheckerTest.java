package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.compound.CompoundChecker;
import org.junit.runners.Parameterized.Parameters;

/** Tests for the compound checker design pattern. */
public class CompoundCheckerTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public CompoundCheckerTest(List<File> testFiles) {
        super(testFiles, CompoundChecker.class, "compound-checker", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"compound-checker"};
    }
}
