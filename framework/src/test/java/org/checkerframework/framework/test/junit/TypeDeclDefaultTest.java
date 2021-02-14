package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.typedecldefault.TypeDeclDefaultChecker;
import org.junit.runners.Parameterized.Parameters;

/** Create the TypeDeclDefault test. */
public class TypeDeclDefaultTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public TypeDeclDefaultTest(List<File> testFiles) {
        super(
                testFiles,
                TypeDeclDefaultChecker.class,
                "typedecldefault",
                "-Anomsgtext",
                "-Astubs=tests/typedecldefault/jdk.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"typedecldefault"};
    }
}
