import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.common.value.qual.MinLen;

public class ZeroMinLen {

    final int @MinLen(1) [] nums;
    final int[] nums2;

    public ZeroMinLen() {
        nums = new int[1];
        nums2 = new int[0];
    }

    @IndexFor("nums") int current_index;

    @IndexFor("nums2") int current_index2;

    void test() {
        current_index = 0;
        //:: error: (assignment.type.incompatible)
        current_index2 = 0;
    }
}
