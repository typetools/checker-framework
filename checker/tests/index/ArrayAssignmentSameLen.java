import org.checkerframework.checker.index.qual.IndexFor;

public class ArrayAssignmentSameLen {

    private final int[] i_array;
    private final @IndexFor("i_array") int i_index;

    ArrayAssignmentSameLen(int[] array, @IndexFor("#1") int index) {
        i_array = array;
        i_index = index;
    }
}
