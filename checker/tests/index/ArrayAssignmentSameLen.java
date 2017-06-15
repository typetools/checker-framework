import org.checkerframework.checker.index.qual.*;

public class ArrayAssignmentSameLen {

    private final int[] i_array;
    private final @IndexFor("i_array") int i_index;

    ArrayAssignmentSameLen(int[] array, @IndexFor("#1") int index) {
        i_array = array;
        i_index = index;
    }

    @SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void test1(int[] a, int[] b, @LTEqLengthOf("#1") int index) {
        int[] array = a;

        @LTLengthOf(
            value = {"array", "b"},
            offset = {"0", "-3"}
        )
        //:: error: (assignment.type.incompatible)
        int i = index;
    }

    @SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void test2(int[] a, int[] b, @LTLengthOf("#1") int i) {
        int[] c = a;
        //:: error: (assignment.type.incompatible)
        @LTLengthOf(value = {"c", "b"}) int x = i;
        @LTLengthOf("c") int y = i;
    }

    @SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void test3(int[] a, @LTLengthOf("#1") int i, @NonNegative int x) {
        int[] c1 = a;
        // See useTest3 for an example of why this return should fail.
        @LTLengthOf(
            value = {"c1", "c1"},
            offset = {"0", "x"}
        )
        //:: error: (assignment.type.incompatible)
        int y = i;
    }

    @SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void test4(
            int[] a,
            @LTLengthOf(
                        value = {"#1", "#1"},
                        offset = {"0", "#3"}
                    )
                    int i,
            @NonNegative int x) {
        int[] c1 = a;
        @LTLengthOf(
            value = {"c1", "c1"},
            offset = {"0", "x"}
        )
        int y = i;
    }

    void useTest3() {
        int[] z = {1, 3};
        test3(z, 0, 10);
    }
}
