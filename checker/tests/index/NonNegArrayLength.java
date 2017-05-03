import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.MinLen;

public class NonNegArrayLength {

    public static void NonNegArrayLength(int @MinLen(4) [] arr) {
        @Positive int i = arr.length - 2;
    }
}
