package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.nontopdefault.NTDChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests the NonTopDefault Checker. */
public class NonTopDefaultTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public NonTopDefaultTest(List<File> testFiles) {
        super(testFiles, NTDChecker.class, "nontopdefault", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nontopdefault"};
    }
}
