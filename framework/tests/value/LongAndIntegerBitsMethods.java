import org.checkerframework.common.value.qual.MinLen;

public class LongAndIntegerBitsMethods {
    void caseInteger(
            int index, int @MinLen(33) [] arr1, int @MinLen(33) [] arr2, int val1, int val2) {
        arr1[Integer.numberOfLeadingZeros(index)] = val1;
        arr2[Integer.numberOfTrailingZeros(index)] = val2;
    }

    void caseLong(int index, int @MinLen(65) [] arr1, int @MinLen(65) [] arr2, int val1, int val2) {
        arr1[Long.numberOfLeadingZeros(index)] = val1;
        arr2[Long.numberOfTrailingZeros(index)] = val2;
    }
}
