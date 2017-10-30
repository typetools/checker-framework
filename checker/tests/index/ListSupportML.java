import java.util.ArrayList;
import org.checkerframework.common.value.qual.MinLen;

// @skip-test until we bring list support back

class ListSupportML {

    void newListMinLen() {
        List<Integer> list = new ArrayList<Integer>();

        // :: error: (assignment.type.incompatible)
        @MinLen(1) List<Integer> list2 = list;

        @MinLen(0) List<Integer> list3 = list;
    }

    void listRemove(@MinLen(10) List<Integer> lst) {
        List<Integer> list = lst;
        list.remove(0);

        // :: error: (assignment.type.incompatible)
        @MinLen(10) List<Integer> list2 = list;

        @MinLen(9) List<Integer> list3 = list;
    }

    void listRemoveAliasing(@MinLen(10) List<Integer> lst) {
        List<Integer> list = lst;
        @MinLen(10) List<Integer> list2 = list;

        list2.remove(0);

        // :: error: (assignment.type.incompatible)
        @MinLen(10) List<Integer> list3 = list;

        @MinLen(9) List<Integer> list4 = list;
    }

    void listAdd(@MinLen(10) List<Integer> lst) {
        List<Integer> list = lst;
        list.add(0);

        @MinLen(11) List<Integer> list2 = list;
    }

    void listClear(@MinLen(10) List<Integer> lst) {
        List<Integer> list = lst;
        list.clear();

        // :: error: (assignment.type.incompatible)
        @MinLen(1) List<Integer> list2 = list;

        @MinLen(0) List<Integer> list3 = list;
    }

    void listRemoveArrayAlter(@MinLen(10) List<Integer> lst) {
        int[] arr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int @MinLen(10) [] arr1 = arr;
        List<Integer> list = lst;
        @MinLen(10) List<Integer> list2 = list;

        list2.remove(0);

        // :: error: (assignment.type.incompatible)
        @MinLen(10) List<Integer> list3 = list;

        int @MinLen(10) [] arr2 = arr;
        @MinLen(9) List<Integer> list4 = list;
    }
}
