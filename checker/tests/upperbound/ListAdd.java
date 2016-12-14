import java.util.*;
import org.checkerframework.checker.index.qual.*;
import org.checkerframework.checker.upperbound.qual.*;

class ListAdd {

    List<Integer> listField;

    void ListAdd(
            @LTLengthOf("list") int index, @LTEqLengthOf("list") int notIndex, List<Integer> list) {
        list.add(index, 4);

        //:: warning: (list.access.unsafe.high)
        list.add(notIndex + 1, 4);
    }

    void ListAddWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        //:: warning: (list.access.unsafe.high)
        list.add(index, 4);
    }

    void ListAddField() {
        listField.add(listField.size() - 1, 4);
        listField.add(this.listField.size() - 1, 4);
        this.listField.add(listField.size() - 1, 4);
        this.listField.add(this.listField.size() - 1, 4);

        //:: warning: (list.access.unsafe.high)
        listField.add(listField.size(), 4);
        //:: warning: (list.access.unsafe.high)
        listField.add(this.listField.size(), 4);
        //:: warning: (list.access.unsafe.high)
        this.listField.add(listField.size(), 4);
        //:: warning: (list.access.unsafe.high)
        this.listField.add(this.listField.size(), 4);
    }

    void ListAddFieldUserAnnotation(@IndexFor("listField") int i) {
        listField.add(i, 4);
        this.listField.add(i, 4);

        //:: warning: (list.access.unsafe.high)
        listField.add(i + 4, 4);
        //:: warning: (list.access.unsafe.high)
        this.listField.add(i + 4, 4);
    }

    void ListAddUserAnnotation(@IndexFor("list") int i, List<Integer> list) {
        list.add(i, 4);

        //:: warning: (list.access.unsafe.high)
        list.add(i + 4, 4);
    }

    void ListAddUpdateValue(List<Integer> list) {
        @LTEqLengthOf("list") int i = list.size();
        @LTLengthOf("list") int r = list.size() - 1;
        list.add(0);
        @LTLengthOf("list") int k = i;
        @LTOMLengthOf("list")
        int p = r;
    }

    void ListAddTwo(
            @LTEqLengthOf({"list", "list2"}) int i, List<Integer> list, List<Integer> list2) {
        @LTEqLengthOf({"list", "list2"}) int j = i;
        list.add(0);
        //:: warning: (list.access.unsafe.high)
        list.get(i);
        //:: warning: (list.access.unsafe.high)
        list2.get(i);
    }
}
