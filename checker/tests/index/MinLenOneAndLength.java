import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.common.value.qual.MinLen;

public class MinLenOneAndLength {
    public @IndexFor("#1") int m1(int @MinLen(1) [] a, int[] b) {
        return a.length / 2;
    }

    public @IndexFor("#1") int m2(int[] b) {
        //:: error: (return.type.incompatible)
        return b.length / 2;
    }
}
