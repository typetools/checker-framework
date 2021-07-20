package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.aggregate.AggregateOfCompoundChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class AggregateTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public AggregateTest(List<File> testFiles) {
        super(
                testFiles,
                AggregateOfCompoundChecker.class,
                "aggregate",
                "-Anomsgtext",
                "-AresolveReflection");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"aggregate"};
    }
}
