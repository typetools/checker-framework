import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.common.value.qual.MinLen;

public class MinLenOneAndLength {
    public void m1(int @MinLen(1) [] a, int[] b) {
        @IndexFor("a") int i = a.length / 2;
        //:: error: (assignment.type.incompatible)
        @IndexFor("b") int j = b.length / 2;
    }
}
