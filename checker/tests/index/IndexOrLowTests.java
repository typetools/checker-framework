import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class IndexOrLowTests {
    final int[] array = {1, 2};

    @IndexOrLow("array") int index = -1;

    //@ SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void test() {

        if (index != -1) {
            array[index] = 1;
        }

        @IndexOrHigh("array") int y = index + 1;
        //:: error: (array.access.unsafe.high)
        array[y] = 1;
        if (y < array.length) {
            array[y] = 1;
        }
        //:: error: (assignment.type.incompatible)
        index = array.length;
    }

    //@ SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void test2(@LTLengthOf("array") @GTENegativeOne int param) {
        index = array.length - 1;
        @LTLengthOf("array") @GTENegativeOne int x = index;
        index = param;
    }
}
