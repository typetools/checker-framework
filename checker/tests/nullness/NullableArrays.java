import org.checkerframework.checker.nullness.qual.*;

public class NullableArrays {
    private byte @Nullable [] padding;

    public NullableArrays(byte @Nullable [] padding) {
        this.padding = padding;
    }
}
