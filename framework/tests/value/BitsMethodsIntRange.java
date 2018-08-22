import org.checkerframework.common.value.qual.IntRange;

public class BitsMethodsIntRange {
    void caseInteger(int integerIndex) {
        @IntRange(from = 0, to = 32) int leadingZeros = Integer.numberOfLeadingZeros(integerIndex);
        @IntRange(from = 0, to = 32) int trailingZeros = Integer.numberOfLeadingZeros(integerIndex);
    }

    void caseLong(long longIndex) {
        @IntRange(from = 0, to = 64) int leadingZeros = Long.numberOfLeadingZeros(longIndex);
        @IntRange(from = 0, to = 64) int trailingZeros = Long.numberOfLeadingZeros(longIndex);
    }
}
