package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Created by jthaine on 6/25/15. */
public class AnnotatedForTest extends CheckerFrameworkPerDirectoryTest {

    public AnnotatedForTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "subtyping",
                "-Anomsgtext",
                "-Aquals=testlib.util.SubQual,testlib.util.SuperQual",
                "-AuseDefaultsForUncheckedCode=source,bytecode");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"conservative-defaults/annotatedfor"};
    }
}
