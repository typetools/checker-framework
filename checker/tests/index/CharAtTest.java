import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.MinLen;

class CharAtTest {
    @NonNegative char test1(@MinLen(1) String s) {
        return s.charAt(0);
    }

    @IntRange(from = 0, to = 65535) char test2(@MinLen(1) String s) {
        return s.charAt(0);
    }
}
