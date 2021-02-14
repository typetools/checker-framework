import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.UpperBoundBottom;

// @skip-test until we bring list support back

public class ListSupport {

    void indexOf(List<Integer> list) {
        int index = list.indexOf(0);

        @LTLengthOf("list") int i = index;

        // :: error: (assignment.type.incompatible)
        @UpperBoundBottom int i2 = index;
    }

    void lastIndexOf(List<Integer> list) {
        int index = list.lastIndexOf(0);

        @LTLengthOf("list") int i = index;

        // :: error: (assignment.type.incompatible)
        @UpperBoundBottom int i2 = index;
    }

    void subList(
            List<Integer> list, @LTLengthOf("#1") int index, @LTEqLengthOf("#1") int endIndex) {
        List<Integer> list2 = list.subList(index, endIndex);

        // start index must be strictly lessthanlength
        // :: error: (argument.type.incompatible)
        list2 = list.subList(endIndex, endIndex);

        // edindex must be less than or equal to Length
        // :: error: (argument.type.incompatible)
        list2 = list.subList(index, 28);
    }

    void size(List<Integer> list) {
        int i = list.size();
        @LTEqLengthOf("list") int k = i;

        // :: error: (assignment.type.incompatible)
        @LTLengthOf("list") int m = i;
    }

    void clear(List<Integer> list) {
        int lessThanLength = list.size() - 1;
        int lessThanOrEq = list.size();
        list.get(lessThanLength);

        list.clear();

        // :: error: (list.access.unsafe.high)
        list.get(lessThanLength);

        // :: error: (assignment.type.incompatible)
        @LTEqLengthOf("list") int m = lessThanLength;

        // :: error: (assignment.type.incompatible)
        m = lessThanOrEq;

        // :: error: (assignment.type.incompatible)
        @LTLengthOf("list") int i = lessThanLength;
    }
}
