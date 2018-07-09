// Test case for issue 62: https://github.com/typetools/checker-framework/issues/62

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

public class EnsuresNonNullIfTest {

    public static void fromDirPos(File1 dbdir) {
        if (dbdir.isDirectory()) {
            File1 @NonNull [] files = dbdir.listFiles();
        }
    }

    public static void fromDirNeg(File1 dbdir) {
        if (!dbdir.isDirectory()) {
            throw new Error("Not a directory: " + dbdir);
        }
        File1 @NonNull [] files = dbdir.listFiles();
    }
}

///////////////////////////////////////////////////////////////////////////
/// Classes copied from the annotated JDK
///

// NOTE:  These annotations are actually incorrect (& not in the JDK).
// But, the test remains valid in how it exercises nullness checking.

// TODO: Have a way of saying the property holds no matter what value is used in a given expression.

class File1 {
    @EnsuresNonNullIf(
            result = true,
            expression = {
                "list()",
                "list(String)", // TODO: has no effect
                "listFiles()",
                "listFiles(String)", // TODO: has no effect
                "listFiles(Double)" // TODO: has no effect
            })
    public boolean isDirectory() {
        throw new RuntimeException("skeleton method");
    }

    public String @Nullable [] list() {
        throw new RuntimeException("skeleton method");
    }

    public String @Nullable [] list(@Nullable String FilenameFilter_a1) {
        throw new RuntimeException("skeleton method");
    }

    public File1 @Nullable [] listFiles() {
        throw new RuntimeException("skeleton method");
    }

    public File1 @Nullable [] listFiles(@Nullable String FilenameFilter_a1) {
        throw new RuntimeException("skeleton method");
    }

    public File1 @Nullable [] listFiles(@Nullable Double FileFilter_a1) {
        throw new RuntimeException("skeleton method");
    }
}
