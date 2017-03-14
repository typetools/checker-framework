import org.checkerframework.checker.index.qual.IndexFor;

public class ArrayAssignmentSameLen {

    private final int[] i_array;
    private final @IndexFor("i_array") int i_index;

    ArrayAssignmentSameLen(int[] array, @IndexFor("#1") int index) {
        i_array = array;
        i_index = index;
    }

    void test1(int[] a, int[] b, @LTEqLengthOf("#1") int index) {
        int[] array = a;
        @LTLengthOf(
            value = {"array", "b"},
            offset = {"0", "-3"}
        )
        int i = index;
    }

    void test2(int[] a, int[] b, @LTLengthOf("#1") int i) {
        int[] c = a;
        //:: error: (assignment.type.incompatible)
        @LTLengthOf(value = {"c", "b"}) int x = i;
        @LTLengthOf("c") int y = i;
    }

    void test3(int[] a, @LTLengthOf("#1") int i, @NonNegative int x) {
        int[] c = a;
        // See useTest3 for an example of why this assignment should fail.
        //:: error: (assignment.type.incompatible)
        @LTLengthOf(
            value = {"c", "c"},
            offset = {"0", "x"}
        )
        int z = i;
    }

    void useTest3() {
        int[] a = {1, 3};
        test3(a, 0, 10);
    }
}
