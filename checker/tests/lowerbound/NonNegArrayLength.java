import org.checkerframework.checker.index.qual.MinLen;
import org.checkerframework.checker.index.qual.Positive;

public class NonNegArrayLength {

    public static void NonNegArrayLength(int @MinLen(4) [] arr) {
        @Positive int i = arr.length - 2;
    }
}
