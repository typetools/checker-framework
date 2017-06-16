import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

class PolyLengthTest {
    int @PolyLength [] id(int @PolyLength [] a) {
        return a;
    }

    int @SameLen("#2") [] test0(int @SameLen("#2") [] a, int @SameLen("#1") [] b) {
        return id(a);
    }

    int @ArrayLen(3) [] test1(int @ArrayLen(3) [] a) {
        return id(a);
    }

    int @MinLen(3) [] test2(int @MinLen(3) [] a) {
        return id(a);
    }
}
