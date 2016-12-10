import org.checkerframework.checker.lowerbound.qual.*;
import org.checkerframework.checker.minlen.qual.*;

public class NonNegArrayLength {

    public static void NonNegArrayLength(int @MinLen(4) [] arr) {
        @Positive int i = arr.length - 2;
    }
}
