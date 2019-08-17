import org.checkerframework.checker.index.qual.NonNegative;

class NonNegativeCharValue {
    public static String toString(final @NonNegative Character ch) {
        return toString(ch.charValue());
    }
}
