import checkers.nullness.quals.*;
import static checkers.nullness.NullnessUtils.*;

class NullableArrays{
    private byte @Nullable [] padding;

    public NullableArrays(byte @Nullable [] padding) {
        this.padding = padding;
    }
}