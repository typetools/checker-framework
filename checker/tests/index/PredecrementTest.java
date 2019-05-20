import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

class PredecrementTest {

    public static void warningForLoop(int @MinLen(1) [] a) {
        for (int i = a.length; --i >= 0; ) {
            a[i] = 0;
        }
    }

    public static void warningIfStatement(int @MinLen(1) [] a) {
        int i = a.length;
        if (--i >= 0) {
            a[i] = 0;
        }
    }

    public static void warningIfStatementRange1(
            int @MinLen(1) [] a, @IntRange(from = 1, to = 1) int j) {
        int i = j;
        if (--i >= 0) {
            a[i] = 0;
        }
    }

    public static void warningIfStatementVal1(int @MinLen(1) [] a, @IntVal(1) int j) {
        int i = j;
        if (--i >= 0) {
            a[i] = 0;
        }
    }

    public static void warningIfStatementRange12(
            int @MinLen(2) [] a, @IntRange(from = 1, to = 2) int j) {
        int i = j;
        if (--i >= 0) {
            a[i] = 0;
        }
    }
}
