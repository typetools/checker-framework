import java.util.*;
import org.checkerframework.checker.index.qual.*;
import org.checkerframework.checker.upperbound.qual.*;

class ListSet {

    List<Integer> listField;

    void ListSet(
            @LTLengthOf("list") int index, @LTEqLengthOf("list") int notIndex, List<Integer> list) {
        list.set(index, 4);

        //:: warning: (list.access.unsafe.high)
        list.set(notIndex, 4);
    }

    void ListSetWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        //:: warning: (list.access.unsafe.high)
        list.set(index, 4);
    }

    void ListSetField() {
        listField.set(listField.size() - 1, 4);
        listField.set(this.listField.size() - 1, 4);
        this.listField.set(listField.size() - 1, 4);
        this.listField.set(this.listField.size() - 1, 4);

        //:: warning: (list.access.unsafe.high)
        listField.set(listField.size(), 4);
        //:: warning: (list.access.unsafe.high)
        listField.set(this.listField.size(), 4);
        //:: warning: (list.access.unsafe.high)
        this.listField.set(listField.size(), 4);
        //:: warning: (list.access.unsafe.high)
        this.listField.set(this.listField.size(), 4);
    }

    void ListSetFieldUserAnnotation(@IndexFor("listField") int i) {
        listField.set(i, 4);
        this.listField.set(i, 4);

        //:: warning: (list.access.unsafe.high)
        listField.set(i + 1, 4);
        //:: warning: (list.access.unsafe.high)
        this.listField.set(i + 1, 4);
    }

    void ListSetUserAnnotation(@IndexFor("list") int i, List<Integer> list) {
        list.set(i, 4);

        //:: warning: (list.access.unsafe.high)
        list.set(i + 1, 4);
    }
}
