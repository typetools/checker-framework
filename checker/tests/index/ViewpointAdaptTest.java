// @skip-test

import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

class ViewpointAdaptTest {

    void ListGet(
            @LTLengthOf("list") int index, @LTEqLengthOf("list") int notIndex, List<Integer> list) {
        // :: error: (argument.type.incompatible)
        list.get(index);

        // :: error: (argument.type.incompatible)
        list.get(notIndex);
    }
}
