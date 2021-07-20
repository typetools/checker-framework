package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Compiler Messages Checker. Depends on the compiler.properties file. */
public class CompilerMessagesTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a CompilerMessagesTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public CompilerMessagesTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.compilermsgs.CompilerMessagesChecker.class,
                "compilermsg",
                "-Anomsgtext",
                "-Apropfiles=tests/compilermsg/compiler.properties");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"compilermsg", "all-systems"};
    }
}
