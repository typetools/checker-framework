import java.util.*;
import org.checkerframework.checker.index.qual.*;
import org.checkerframework.checker.upperbound.qual.*;

class ListSupport {

    void indexOf(List<Integer> list) {
        int index = list.indexOf(0);

        @LTLengthOf("list") int i = index;

        //:: error: (assignment.type.incompatible)
        @UpperBoundBottom int i2 = index;
    }

    void lastIndexOf(List<Integer> list) {
        int index = list.lastIndexOf(0);

        @LTLengthOf("list") int i = index;

        //:: error: (assignment.type.incompatible)
        @UpperBoundBottom int i2 = index;
    }

    void subList(
            List<Integer> list, @LTLengthOf("list") int index, @LTEqLengthOf("list") int endIndex) {
        List<Integer> list2 = list.subList(index, endIndex);

        // start index must be strictly lessthanlength
        //:: error: (argument.type.incompatible)
        list2 = list.subList(endIndex, endIndex);

        // edindex must be less than or equal to Length
        //:: error: (argument.type.incompatible)
        list2 = list.subList(index, 28);
    }

    void size(List<Integer> list) {
        int i = list.size();
        @LTEqLengthOf("list") int k = i;

        //:: error: (assignment.type.incompatible)
        @LTLengthOf("list") int m = i;
    }
}
