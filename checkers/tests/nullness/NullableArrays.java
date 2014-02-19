import checkers.nullness.quals.*;

class NullableArrays{
    private byte @Nullable [] padding;

    public NullableArrays(byte @Nullable [] padding) {
        this.padding = padding;
    }
}