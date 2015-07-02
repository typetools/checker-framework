import org.checkerframework.checker.nullness.qual.*;

class NullableArrays {
    private byte @Nullable [] padding;

    public NullableArrays(byte @Nullable [] padding) {
        this.padding = padding;
    }
}
