package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Created by jthaine on 6/25/15. */
public class AnnotatedForTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public AnnotatedForTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "subtyping",
                "-Anomsgtext",
                "-Aquals=org.checkerframework.framework.testchecker.util.SubQual,org.checkerframework.framework.testchecker.util.SuperQual",
                "-AuseConservativeDefaultsForUncheckedCode=source,bytecode");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"conservative-defaults/annotatedfor"};
    }
}
