import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java17-jdk-skip-test
public record BasicRecordNullable(@Nullable String str) {

    public static BasicRecordNullable makeNonNull(String s) {
        return new BasicRecordNullable(s);
    }

    public static BasicRecordNullable makeNull(@Nullable String s) {
        return new BasicRecordNullable(s);
    }

    public @Nullable String getStringFromField() {
        return str;
    }

    public @Nullable String getStringFromMethod() {
        return str();
    }

    public String getStringFromFieldErr() {
        // :: error: (return.type.incompatible)
        return str;
    }

    public String getStringFromMethodErr() {
        // :: error: (return.type.incompatible)
        return str();
    }
}
