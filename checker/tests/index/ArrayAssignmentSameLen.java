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
}
