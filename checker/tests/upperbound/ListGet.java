import java.util.*;
import org.checkerframework.checker.index.qual.*;
import org.checkerframework.checker.upperbound.qual.*;

class ListGet {

    List<Integer> listField;
    int[] arr = {0};

    void ListGet(
            @LTLengthOf("#3") int index, @LTEqLengthOf("#3") int notIndex, List<Integer> list) {
        list.get(index);

        //:: warning: (list.access.unsafe.high)
        list.get(notIndex);
    }

    void ListGetWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        //:: warning: (list.access.unsafe.high)
        list.get(index);
    }

    void ListGetField() {
        listField.get(listField.size() - 1);
        listField.get(this.listField.size() - 1);
        this.listField.get(listField.size() - 1);
        this.listField.get(this.listField.size() - 1);

        //:: warning: (list.access.unsafe.high)
        listField.get(listField.size());
        //:: warning: (list.access.unsafe.high)
        listField.get(this.listField.size());
        //:: warning: (list.access.unsafe.high)
        this.listField.get(listField.size());
        //:: warning: (list.access.unsafe.high)
        this.listField.get(this.listField.size());
    }

    void ListGetFieldUserAnnotation(@IndexFor("listField") int i) {
        listField.get(i);
        this.listField.get(i);

        //:: warning: (list.access.unsafe.high)
        listField.get(i + 1);
        //:: warning: (list.access.unsafe.high)
        this.listField.get(i + 1);
    }

    void ListGetUserAnnotation(@IndexFor("#2") int i, List<Integer> list) {
        list.get(i);

        //:: warning: (list.access.unsafe.high)
        list.get(i + 1);
    }
}
