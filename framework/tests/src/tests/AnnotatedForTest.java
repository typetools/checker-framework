package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * Created by jthaine on 6/25/15.
 */
public class AnnotatedForTest extends DefaultCheckerTest {

    public AnnotatedForTest(File testFile) {
        super(testFile,
              org.checkerframework.common.subtyping.SubtypingChecker.class,
              "subtyping",
              "-Anomsgtext",
              "-Aquals=tests.util.SubQual,tests.util.SuperQual",
              "-AuseSafeDefaultsForUnannotatedSourceCode");
    }



    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"conservative-defaults/annotatedfor"};
    }
}
