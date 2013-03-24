import checkers.nullness.quals.*;
import static checkers.nullness.util.NonNullUtils.*;

class NullableArrays{
    private byte @Nullable [] padding;

    public NullableArrays(byte @Nullable [] padding) {
        this.padding = padding;
    }
}