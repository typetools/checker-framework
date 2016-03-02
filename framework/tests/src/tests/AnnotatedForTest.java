package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

/**
 * Created by jthaine on 6/25/15.
 */
public class AnnotatedForTest extends CheckerFrameworkTest {

    public AnnotatedForTest(File testFile) {
        super(testFile,
              org.checkerframework.common.subtyping.SubtypingChecker.class,
              "subtyping",
              "-Anomsgtext",
              "-Aquals=tests.util.SubQual,tests.util.SuperQual",
              "-AuseDefaultsForUncheckedCode=source,bytecode");
    }



    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"conservative-defaults/annotatedfor"};
    }
}
