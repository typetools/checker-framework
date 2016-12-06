import java.util.*;
import org.checkerframework.checker.upperbound.qual.*;
// @skip-test
class ListSupport {

    void ListGet(
            @LTLengthOf("list") int index, @LTEqLengthOf("list") int notIndex, List<Integer> list) {
        list.get(index);

        //:: error: (argument.type.incompatible)
        list.get(notIndex);
    }

    void ListGetWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        //:: error: (argument.type.imcompatible)
        list.get(index);
    }

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

    void remove(
            List<Integer> list, @LTLengthOf("list") int index, @LTEqLengthOf("list") int endIndex) {
        int i = index;
        int j = endIndex;
        list.remove(0);

        //:: error: (assignment.type.incompatible)
        @LTLengthOf("list") int less = i;

        @LTEqLengthOf("list") int loe = i;

        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf("list") int loe2 = j;
    }

    void clear(
            List<Integer> list, @LTLengthOf("list") int index, @LTEqLengthOf("list") int endIndex) {
        int i = index;
        int j = endIndex;
        list.clear();

        //:: error: (assignment.type.incompatible)
        @LTLengthOf("list") int less = i;

        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf("list") int loe = i;

        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf("list") int loe2 = j;
    }

    void add(
            List<Integer> list,
            @LTEqLengthOf("list") int index,
            @LTEqLengthOf("arr") int arrIndex) {
        int i = index;
        int arri = arrIndex;

        list.add(0);
        @LTLengthOf("list") int less = i;

        //:: error: (assignment.type.incompatible)
        @LTLengthOf("arr") int arrLess = arri;
    }
}
