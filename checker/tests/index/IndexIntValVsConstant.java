import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.IntVal;

public class IndexIntValVsConstant {

    void m() {

        int @ArrayLen(7) [] a1 = new int[] {1, 2, 3, 4, 5, 6, 7};

        @IntVal(2) int i = 2;
        @IntVal(4) int j = 4;

        int[] s0 = internSubsequence(a1, 2, 4);
        int[] s1 = internSubsequence(a1, i, j);
    }

    int @Interned [] internSubsequence(
            int @Interned [] seq,
            @IndexFor("#1") @LessThan("#3") int start,
            @NonNegative @LTLengthOf(value = "#1", offset = "#2 - 1") int end) {
        // dummy implementation
        return new int[0];
    }
}
