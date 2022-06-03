package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.typedeclbounds.TypeDeclBoundsChecker;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

public class TypeDeclBoundsTest extends CheckerFrameworkPerDirectoryTest {

    public TypeDeclBoundsTest(List<File> testFiles) {
        super(testFiles, TypeDeclBoundsChecker.class, "typedeclbounds");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"typedeclbounds"};
    }
}
