package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Test suite for the Subtyping Checker, using a simple {@link Encrypted} annotation. */
public class SubtypingEncryptedTest extends CheckerFrameworkPerDirectoryTest {

    public SubtypingEncryptedTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "subtyping",
                "-Anomsgtext",
                "-Aquals=testlib.util.Encrypted,testlib.util.PolyEncrypted,org.checkerframework.framework.qual.Unqualified");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"subtyping", "all-systems"};
    }
}
