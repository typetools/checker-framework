import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java17-jdk-skip-test
public class LocalRecords {
    public static void foo() {
        record L(String key, @Nullable Integer value) {}
        L a = new L("one", 1);
        L b = new L("i", null);
        // :: error: (argument.type.incompatible)
        L c = new L(null, 6);
    }
}
