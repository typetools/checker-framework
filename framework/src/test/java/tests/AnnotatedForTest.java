package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Created by jthaine on 6/25/15. */
public class AnnotatedForTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
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
